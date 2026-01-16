package com.screenleads.backend.app.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.repositories.PromotionRedemptionRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;
import com.screenleads.backend.app.web.dto.LeadSummaryDTO;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import com.screenleads.backend.app.web.dto.PromotionLeadDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private static final String PROMOTION_NOT_FOUND = "Promotion not found: ";
    private static final String LEAD_ALREADY_EXISTS = "Lead already exists for identifier: ";

    private final PromotionRepository promotionRepository;
    private final ObjectMapper objectMapper; // Autoconfigurado por Spring Boot
    private final StripeBillingService billingService;

    // =========================================
    // CRUD Promotion
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll()
                .stream()
                .map(p -> map(p, PromotionDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDTO getPromotionById(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PROMOTION_NOT_FOUND + id));
        return map(p, PromotionDTO.class);
    }

    @Override
    public PromotionDTO savePromotion(@Nullable PromotionDTO dto) {
        // Mapear DTO -> Entity sin suponer getters concretos
        Promotion toSave = map(dto, Promotion.class);
        Promotion saved = promotionRepository.save(toSave);
        return map(saved, PromotionDTO.class);
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PROMOTION_NOT_FOUND + id));

        // Creamos un "patch" a partir del DTO y fusionamos solo campos no nulos
        Promotion patch = map(dto, Promotion.class);
        mergeNonNull(patch, existing);

        // El entity está gestionado en la sesión; devolver mapeado a DTO
        return map(existing, PromotionDTO.class);
    }

    @Override
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException(PROMOTION_NOT_FOUND + id);
        }
        promotionRepository.deleteById(id);
    }

    // =========================================
    // Leads - DEPRECATED: Usar PromotionRedemptionService
    // =========================================

    @Override
    @Deprecated
    public PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto) {
        throw new UnsupportedOperationException("This method is deprecated. Use PromotionRedemptionService instead.");
    }

    @Override
    @Deprecated
    public List<PromotionLeadDTO> listLeads(Long promotionId) {
        throw new UnsupportedOperationException("This method is deprecated. Use PromotionRedemptionService instead.");
    }

    @Override
    @Deprecated
    public String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        throw new UnsupportedOperationException("This method is deprecated. Use PromotionRedemptionService instead.");
    }

    @Override
    @Deprecated
    public LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        throw new UnsupportedOperationException("This method is deprecated. Use PromotionRedemptionService instead.");
    }

    @Override
    @Deprecated
    public PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides) {
        throw new UnsupportedOperationException("This method is deprecated. Use PromotionRedemptionService instead.");
    }

    // =========================================
    // Helpers
    // =========================================

    private <T> T map(Object source, Class<T> targetType) {
        if (source == null)
            return null;
        return objectMapper.convertValue(source, targetType);
    }

    private static void mergeNonNull(Object src, Object target) {
        if (src == null || target == null)
            return;
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String name = pd.getName();
            // ignorar "class"
            if ("class".equals(name))
                continue;
            Object value = src.getPropertyValue(name);
            if (value == null) {
                emptyNames.add(name);
            }
        }
        return emptyNames.toArray(new String[0]);
    }
}

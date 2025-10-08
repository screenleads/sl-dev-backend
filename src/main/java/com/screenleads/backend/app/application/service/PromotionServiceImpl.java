package com.screenleads.backend.app.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.model.PromotionLead;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;
import com.screenleads.backend.app.web.dto.LeadSummaryDTO;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import com.screenleads.backend.app.web.dto.PromotionLeadDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionLeadRepository promotionLeadRepository;
    private final ObjectMapper objectMapper; // Autoconfigurado por Spring Boot

    // =========================================
    // CRUD Promotion
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll()
                .stream()
                .map(p -> map(p, PromotionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDTO getPromotionById(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        return map(p, PromotionDTO.class);
    }

    @Override
    public PromotionDTO savePromotion(PromotionDTO dto) {
        // Mapear DTO -> Entity sin suponer getters concretos
        Promotion toSave = map(dto, Promotion.class);
        Promotion saved = promotionRepository.save(toSave);
        return map(saved, PromotionDTO.class);
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));

        // Creamos un "patch" a partir del DTO y fusionamos solo campos no nulos
        Promotion patch = map(dto, Promotion.class);
        mergeNonNull(patch, existing);

        // El entity está gestionado en la sesión; devolver mapeado a DTO
        return map(existing, PromotionDTO.class);
    }

    @Override
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Promotion not found: " + id);
        }
        promotionRepository.deleteById(id);
    }

    // =========================================
    // Leads
    // =========================================

    @Override
    public PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Mapear DTO -> Entity temporalmente para leer campos como identifier sin usar
        // getters del DTO
        PromotionLead candidate = map(dto, PromotionLead.class);

        // Si tienes unique (promotion_id + identifier), prevenimos duplicados
        if (candidate.getIdentifier() != null &&
                promotionLeadRepository.existsByPromotionIdAndIdentifier(promotionId, candidate.getIdentifier())) {
            throw new IllegalArgumentException("Lead already exists for identifier: " + candidate.getIdentifier());
        }

        candidate.setPromotion(promo);
        PromotionLead saved = promotionLeadRepository.save(candidate);
        return map(saved, PromotionLeadDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionLeadDTO> listLeads(Long promotionId) {
        return promotionLeadRepository.findByPromotionId(promotionId).stream()
                .map(l -> map(l, PromotionLeadDTO.class))
                .collect(Collectors.toList());
    }

    // =========================================
    // Export / Informes
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        Instant fromI = from != null ? from.toInstant() : Instant.EPOCH;
        Instant toI = to != null ? to.toInstant() : Instant.now();

        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId).stream()
                .filter(l -> {
                    Instant c = l.getCreatedAt();
                    return (c.equals(fromI) || c.isAfter(fromI)) && (c.equals(toI) || c.isBefore(toI));
                })
                .sorted(Comparator.comparing(PromotionLead::getCreatedAt))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(
                "id,promotionId,identifierType,identifier,firstName,lastName,email,phone,birthDate,acceptedPrivacyAt,acceptedTermsAt,createdAt\n");
        for (PromotionLead l : leads) {
            sb.append(Optional.ofNullable(l.getId()).orElse(0L)).append(',')
                    .append(Optional.ofNullable(l.getPromotion()).map(Promotion::getId).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getIdentifierType()).map(Enum::name).orElse("")).append(',')
                    .append(csv(l.getIdentifier())).append(',')
                    .append(csv(l.getFirstName())).append(',')
                    .append(csv(l.getLastName())).append(',')
                    .append(csv(l.getEmail())).append(',')
                    .append(csv(l.getPhone())).append(',')
                    .append(Optional.ofNullable(l.getBirthDate()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getAcceptedPrivacyAt()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getAcceptedTermsAt()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getCreatedAt()).orElse(null))
                    .append('\n');
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant fromI = from != null ? from.toInstant() : Instant.EPOCH;
        Instant toI = to != null ? to.toInstant() : Instant.now();

        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId).stream()
                .filter(l -> {
                    Instant c = l.getCreatedAt();
                    return (c.equals(fromI) || c.isAfter(fromI)) && (c.equals(toI) || c.isBefore(toI));
                })
                .toList();

        long totalLeads = leads.size();
        long uniqueIdentifiers = leads.stream()
                .map(PromotionLead::getIdentifier)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<LocalDate, Long> leadsByDay = leads.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        l -> l.getCreatedAt().atZone(zone).toLocalDate(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()));

        return new LeadSummaryDTO(promotionId, totalLeads, uniqueIdentifiers, leadsByDay);
    }

    @Override
    public PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Base "falsa" en Entity
        PromotionLead base = PromotionLead.builder()
                .identifierType(LeadIdentifierType.EMAIL)
                .identifier(UUID.randomUUID() + "@example.test")
                .firstName("Test")
                .lastName("Lead")
                .email(null) // lo puede aportar overrides
                .acceptedPrivacyAt(Instant.now())
                .acceptedTermsAt(Instant.now())
                .promotion(promo)
                .build();

        if (overrides != null) {
            PromotionLead patch = map(overrides, PromotionLead.class);
            mergeNonNull(patch, base);
        }

        PromotionLead saved = promotionLeadRepository.save(base);
        return map(saved, PromotionLeadDTO.class);
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

    private static String csv(String s) {
        if (s == null)
            return "";
        String escaped = s.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}

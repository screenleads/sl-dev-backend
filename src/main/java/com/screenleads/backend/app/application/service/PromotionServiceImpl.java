package com.screenleads.backend.app.application.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.LeadLimitType;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.model.PromotionLead;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl {

    private final PromotionRepository promotionRepository;
    private final PromotionLeadRepository promotionLeadRepository;

    // ==== Ejemplo de método que leía los enums y legalUrl (ajústalo a tu API) ====

    public Promotion createPromotion(Promotion dto) {
        // DTO aquí es entity para simplificar; si usas DTOs, mapea antes.
        Promotion p = Promotion.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .legalUrl(dto.getLegalUrl())                  // ✅ camelCase
                .url(dto.getUrl())
                .templateHtml(dto.getTemplateHtml())
                .leadIdentifierType(dto.getLeadIdentifierType()) // ✅ enums en el modelo
                .leadLimitType(dto.getLeadLimitType())
                .company(dto.getCompany())
                .build();
        return promotionRepository.save(p);
    }

    public Promotion updatePromotion(Long id, Promotion dto) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setLegalUrl(dto.getLegalUrl());                   // ✅ camelCase
        p.setUrl(dto.getUrl());
        p.setTemplateHtml(dto.getTemplateHtml());
        if (dto.getLeadIdentifierType() != null) {
            p.setLeadIdentifierType(dto.getLeadIdentifierType());
        }
        if (dto.getLeadLimitType() != null) {
            p.setLeadLimitType(dto.getLeadLimitType());
        }
        return p; // managed entity
    }

    // ==== Ejemplo de alta de lead usando builder del ENTITY PromotionLead ====

    public PromotionLead addLead(Long promotionId, PromotionLead payload) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        PromotionLead lead = PromotionLead.builder()
                .promotion(promo)
                .firstName(payload.getFirstName())
                .lastName(payload.getLastName())
                .email(payload.getEmail())
                .phone(payload.getPhone())
                .birthDate(payload.getBirthDate())
                .acceptedPrivacyAt(payload.getAcceptedPrivacyAt())
                .acceptedTermsAt(payload.getAcceptedTermsAt())
                // si tu modelo usa identificador/limitType a nivel de lead:
                .identifier(payload.getIdentifier())
                .identifierType(payload.getIdentifierType())
                .limitType(payload.getLimitType())
                .build();

        return promotionLeadRepository.save(lead);
    }

    // ==== Ejemplo de agregación por día (Instant -> ZonedDateTime, NO withZoneSameInstant en Instant) ====

    @Transactional(readOnly = true)
    public TreeMap<ZonedDateTime, Long> leadsPerDay(Long promotionId, ZoneId zoneId) {
        ZoneId zone = Objects.requireNonNullElse(zoneId, ZoneId.systemDefault());

        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId);

        return leads.stream().collect(Collectors.groupingBy(
                l -> l.getCreatedAt().atZone(zone)      // createdAt: Instant (Auditable) -> ZonedDateTime
                         .toLocalDate()
                         .atStartOfDay(zone),
                TreeMap::new,
                Collectors.counting()
        ));
    }

    // ==== Helpers de lectura de enums para que no fallen los getters en cualquier sitio ====

    @Transactional(readOnly = true)
    public LeadIdentifierType getIdentifierType(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .map(Promotion::getLeadIdentifierType)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));
    }

    @Transactional(readOnly = true)
    public LeadLimitType getLimitType(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .map(Promotion::getLeadLimitType)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));
    }
}

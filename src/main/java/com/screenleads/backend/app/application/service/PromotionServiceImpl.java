package com.screenleads.backend.app.application.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.IdentifierNormalizer;
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

    // =========================================================================
    //                               PROMOTIONS
    // =========================================================================

    public Promotion createPromotion(Promotion dto) {
        // Si usas DTOs separados, mapéalos antes.
        Promotion p = Promotion.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .legalUrl(dto.getLegalUrl())           // camelCase correcto
                .url(dto.getUrl())
                .templateHtml(dto.getTemplateHtml())
                .leadIdentifierType(dto.getLeadIdentifierType())
                .leadLimitType(defaultLimit(dto.getLeadLimitType()))
                .company(dto.getCompany())
                .build();
        return promotionRepository.save(p);
    }

    public Promotion updatePromotion(Long id, Promotion dto) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setLegalUrl(dto.getLegalUrl());
        p.setUrl(dto.getUrl());
        p.setTemplateHtml(dto.getTemplateHtml());

        if (dto.getLeadIdentifierType() != null) {
            p.setLeadIdentifierType(dto.getLeadIdentifierType());
        }
        if (dto.getLeadLimitType() != null) {
            p.setLeadLimitType(defaultLimit(dto.getLeadLimitType()));
        }
        // Entidad gestionada: se sincroniza al commit
        return p;
    }

    // =========================================================================
    //                                  LEADS
    // =========================================================================

    public PromotionLead addLead(Long promotionId, PromotionLead payload) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Resolver tipos con fallback a la promoción (y defaults)
        LeadIdentifierType resolvedIdType = resolveIdentifierType(payload.getIdentifierType(), promo.getLeadIdentifierType());
        LeadLimitType resolvedLimitType   = resolveLimitType(payload.getLimitType(), promo.getLeadLimitType());

        // Normalizar el identifier según tipo
        String rawIdentifier = payload.getIdentifier();
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new IllegalArgumentException("identifier is required");
        }
        String normalizedIdentifier = IdentifierNormalizer.normalize(resolvedIdType, rawIdentifier);

        // Pre-chequeo de unicidad para mensaje de error amistoso
        if (promotionLeadRepository.existsByPromotionIdAndIdentifier(promotionId, normalizedIdentifier)) {
            throw new IllegalStateException("Lead already exists for this promotion and identifier");
        }

        PromotionLead lead = PromotionLead.builder()
                .promotion(promo)
                // Datos personales (opcional según tu modelo/uso)
                .firstName(payload.getFirstName())
                .lastName(payload.getLastName())
                .email(payload.getEmail())
                .phone(payload.getPhone())
                .birthDate(payload.getBirthDate())
                .acceptedPrivacyAt(payload.getAcceptedPrivacyAt())
                .acceptedTermsAt(payload.getAcceptedTermsAt())
                // Identificador + límites resueltos/normalizados
                .identifierType(resolvedIdType)
                .identifier(normalizedIdentifier)
                .limitType(resolvedLimitType)
                .build();

        try {
            return promotionLeadRepository.save(lead);
        } catch (DataIntegrityViolationException dive) {
            // Por si otro proceso ganó la carrera y saltó la unique constraint DB
            throw new IllegalStateException("Lead already exists for this promotion and identifier", dive);
        }
    }

    @Transactional(readOnly = true)
    public TreeMap<ZonedDateTime, Long> leadsPerDay(Long promotionId, ZoneId zoneId) {
        ZoneId zone = Objects.requireNonNullElse(zoneId, ZoneId.systemDefault());
        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId);

        return leads.stream().collect(Collectors.groupingBy(
                l -> l.getCreatedAt()                 // Instant (Auditable)
                        .atZone(zone)
                        .toLocalDate()
                        .atStartOfDay(zone),
                TreeMap::new,
                Collectors.counting()
        ));
    }

    // =========================================================================
    //                               HELPERS
    // =========================================================================

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

    private static LeadLimitType defaultLimit(LeadLimitType value) {
        return value != null ? value : LeadLimitType.NO_LIMIT;
    }

    private static LeadIdentifierType resolveIdentifierType(LeadIdentifierType fromPayload,
                                                            LeadIdentifierType fromPromotion) {
        if (fromPayload != null) return fromPayload;
        if (fromPromotion != null) return fromPromotion;
        // Por defecto, si no viene ni en payload ni en promo:
        return LeadIdentifierType.OTHER;
    }

    private static LeadLimitType resolveLimitType(LeadLimitType fromPayload,
                                                  LeadLimitType fromPromotion) {
        if (fromPayload != null) return fromPayload;
        if (fromPromotion != null) return fromPromotion;
        return LeadLimitType.NO_LIMIT;
    }
}

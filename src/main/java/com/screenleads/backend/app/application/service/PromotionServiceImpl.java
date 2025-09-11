package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;
import com.screenleads.backend.app.web.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private PromotionLeadRepository promotionLeadRepository;

    // ===== CRUD =====

    @Override
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public PromotionDTO getPromotionById(Long id) {
        return promotionRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Override
    public PromotionDTO savePromotion(PromotionDTO dto) {
        Promotion entity = toEntity(dto);
        return toDto(promotionRepository.save(entity));
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion entity = toEntity(dto);
        entity.setId(id);
        return toDto(promotionRepository.save(entity));
    }

    @Override
    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    // ===== Leads =====

    @Override
    public PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        String identifier = normalizeIdentifier(promo.getLeadIdentifierType(), dto);

        if (promo.getLeadIdentifierType() == LeadIdentifierType.EMAIL &&
                (dto.email() == null || dto.email().isBlank())) {
            throw new IllegalArgumentException("Email es obligatorio para esta promoción");
        }
        if (promo.getLeadIdentifierType() == LeadIdentifierType.PHONE &&
                (dto.phone() == null || dto.phone().isBlank())) {
            throw new IllegalArgumentException("Teléfono es obligatorio para esta promoción");
        }

        switch (promo.getLeadLimitType()) {
            case ONE_PER_PERSON -> promotionLeadRepository
                    .findTopByPromotion_IdAndIdentifierOrderByCreatedAtDesc(promotionId, identifier)
                    .ifPresent(l -> {
                        throw new IllegalStateException("Ya existe un registro para esta persona en esta promoción");
                    });
            case ONE_PER_24H -> {
                ZonedDateTime cutoff = ZonedDateTime.now().minusHours(24);
                long recent = promotionLeadRepository
                        .countByPromotion_IdAndIdentifierAndCreatedAtAfter(promotionId, identifier, cutoff);
                if (recent > 0)
                    throw new IllegalStateException("Solo se permite un registro cada 24 horas para esta promoción");
            }
            case NO_LIMIT -> {
                /* sin restricciones */ }
        }

        PromotionLead lead = PromotionLead.builder()
                .promotion(promo)
                .identifier(identifier)
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email() != null ? dto.email().trim() : null)
                .phone(normalizePhone(dto.phone()))
                .birthDate(dto.birthDate())
                .acceptedPrivacyAt(dto.acceptedPrivacyAt())
                .acceptedTermsAt(dto.acceptedTermsAt())
                .createdAt(dto.createdAt() != null ? dto.createdAt() : ZonedDateTime.now())
                .build();

        PromotionLead saved = promotionLeadRepository.save(lead);
        return toDto(saved);
    }

    @Override
    public List<PromotionLeadDTO> listLeads(Long promotionId) {
        return promotionLeadRepository.findByPromotion_IdOrderByCreatedAtDesc(promotionId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ===== Informes / Export =====

    @Override
    public String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        ensurePromoExists(promotionId);
        List<PromotionLead> leads = promotionLeadRepository
                .findByPromotion_IdAndCreatedAtBetweenOrderByCreatedAtAsc(promotionId, from, to);

        StringBuilder sb = new StringBuilder();
        sb.append(
                "id,promotionId,firstName,lastName,email,phone,birthDate,acceptedPrivacyAt,acceptedTermsAt,createdAt,identifier\n");
        for (PromotionLead l : leads) {
            sb.append(csv(l.getId())).append(',')
                    .append(csv(l.getPromotion().getId())).append(',')
                    .append(csv(l.getFirstName())).append(',')
                    .append(csv(l.getLastName())).append(',')
                    .append(csv(l.getEmail())).append(',')
                    .append(csv(l.getPhone())).append(',')
                    .append(csv(l.getBirthDate())).append(',')
                    .append(csv(l.getAcceptedPrivacyAt())).append(',')
                    .append(csv(l.getAcceptedTermsAt())).append(',')
                    .append(csv(l.getCreatedAt())).append(',')
                    .append(csv(l.getIdentifier())).append('\n');
        }
        return sb.toString();
    }

    @Override
    public LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        ensurePromoExists(promotionId);
        List<PromotionLead> leads = promotionLeadRepository
                .findByPromotion_IdAndCreatedAtBetweenOrderByCreatedAtAsc(promotionId, from, to);

        long total = leads.size();
        long uniques = leads.stream().map(PromotionLead::getIdentifier).distinct().count();

        Map<LocalDate, Long> byDay = leads.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getCreatedAt().withZoneSameInstant(ZoneId.systemDefault()).toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()));

        return new LeadSummaryDTO(promotionId, total, uniques, byDay);
    }

    // ===== Lead de prueba =====

    @Override
    public PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        long ts = System.currentTimeMillis();
        String first = "Test";
        String last = "Lead" + (ts % 100000);

        PromotionLeadDTO base = new PromotionLeadDTO(
                null,
                promotionId,
                first,
                last,
                "test" + ts + "@example.com",
                "600" + String.valueOf(ts).substring(Math.max(0, String.valueOf(ts).length() - 6)), // 600xxxxxx
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now());

        PromotionLeadDTO dto = new PromotionLeadDTO(
                null,
                promotionId,
                overrides != null && overrides.firstName() != null ? overrides.firstName() : base.firstName(),
                overrides != null && overrides.lastName() != null ? overrides.lastName() : base.lastName(),
                overrides != null && overrides.email() != null ? overrides.email() : base.email(),
                overrides != null && overrides.phone() != null ? overrides.phone() : base.phone(),
                overrides != null ? overrides.birthDate() : base.birthDate(),
                base.acceptedPrivacyAt(),
                base.acceptedTermsAt(),
                base.createdAt());

        return registerLead(promotionId, dto);
    }

    // ===== Utilidades =====

    private void ensurePromoExists(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new RuntimeException("Promotion not found");
        }
    }

    private String csv(Object value) {
        if (value == null)
            return "";
        String s = String.valueOf(value);
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (needsQuotes)
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private PromotionDTO toDto(Promotion e) {
        return new PromotionDTO(
                e.getId(),
                e.getLegal_url(),
                e.getUrl(),
                e.getDescription(),
                e.getTemplateHtml(),
                e.getLeadLimitType(),
                e.getLeadIdentifierType());
    }

    private Promotion toEntity(PromotionDTO d) {
        return Promotion.builder()
                .id(d.id())
                .legal_url(d.legal_url())
                .url(d.url())
                .description(d.description())
                .templateHtml(d.templateHtml())
                .leadLimitType(d.leadLimitType() != null ? d.leadLimitType() : LeadLimitType.NO_LIMIT)
                .leadIdentifierType(d.leadIdentifierType() != null ? d.leadIdentifierType() : LeadIdentifierType.EMAIL)
                .build();
    }

    private PromotionLeadDTO toDto(PromotionLead e) {
        return new PromotionLeadDTO(
                e.getId(),
                e.getPromotion().getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getPhone(),
                e.getBirthDate(),
                e.getAcceptedPrivacyAt(),
                e.getAcceptedTermsAt(),
                e.getCreatedAt());
    }

    private String normalizeIdentifier(LeadIdentifierType type, PromotionLeadDTO dto) {
        return switch (type) {
            case EMAIL -> {
                if (dto.email() == null)
                    throw new IllegalArgumentException("Email requerido");
                yield dto.email().trim().toLowerCase();
            }
            case PHONE -> {
                if (dto.phone() == null)
                    throw new IllegalArgumentException("Teléfono requerido");
                yield normalizePhone(dto.phone());
            }
        };
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        return phone.replaceAll("[\\s-]", "");
    }
}

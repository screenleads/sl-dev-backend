package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionsController {
    private static final String EUROPE_MADRID = "Europe/Madrid";

    private final PromotionService promotionService;

    // ===== CRUD =====
    @PreAuthorize("@perm.can('promotion', 'read')")
    @GetMapping
    public List<PromotionDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @PreAuthorize("@perm.can('promotion', 'read')")
    @GetMapping("/{id}")
    public PromotionDTO getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id);
    }

    @PreAuthorize("@perm.can('promotion', 'write')")
    @PostMapping
    public PromotionDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.savePromotion(promotionDTO);
    }

    @PreAuthorize("@perm.can('promotion', 'write')")
    @PutMapping("/{id}")
    public PromotionDTO updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @PreAuthorize("@perm.can('promotion', 'delete')")
    @DeleteMapping("/{id}")
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }

    // ===== Leads =====
    @PreAuthorize("@perm.can('lead', 'write')")
    @PostMapping("/{id}/leads")
    public PromotionLeadDTO registerLead(@PathVariable Long id, @RequestBody PromotionLeadDTO leadDTO) {
        return promotionService.registerLead(id, leadDTO);
    }

    @PreAuthorize("@perm.can('lead', 'read')")
    @GetMapping("/{id}/leads")
    public List<PromotionLeadDTO> listLeads(@PathVariable Long id) {
        return promotionService.listLeads(id);
    }

    // ===== Lead de prueba =====
    @PreAuthorize("@perm.can('lead', 'write')")
    @PostMapping("/{id}/leads/test")
    public PromotionLeadDTO createTestLead(
            @PathVariable Long id,
            @RequestBody(required = false) PromotionLeadDTO overrides) {
        return promotionService.createTestLead(id, overrides);
    }

    // ===== Export CSV (Streaming) =====
    @PreAuthorize("@perm.can('lead', 'read')")
    @GetMapping(value = "/{id}/leads/export.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportLeadsCsv(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of(EUROPE_MADRID)));
        ZonedDateTime fromZdt = parseZdtOrDefault(from, toZdt.minusDays(30));

        StreamingResponseBody body = outputStream -> {
            String csv = promotionService.exportLeadsCsv(id, fromZdt, toZdt);
            outputStream.write(csv.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        };

        String filename = "promotion-" + id + "-leads.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    // ===== Resumen JSON =====
    @GetMapping("/{id}/leads/summary")
    public LeadSummaryDTO getLeadSummary(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of(EUROPE_MADRID)));
        ZonedDateTime fromZdt = parseZdtOrDefault(from, toZdt.minusDays(30));
        return promotionService.getLeadSummary(id, fromZdt, toZdt);
    }

    private ZonedDateTime parseZdtOrDefault(String s, ZonedDateTime defaultValue) {
        if (s == null || s.isBlank())
            return defaultValue;
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return d.atStartOfDay(ZoneId.of(EUROPE_MADRID));
            }
            return ZonedDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }
}

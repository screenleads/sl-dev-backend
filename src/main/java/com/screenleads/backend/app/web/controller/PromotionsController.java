package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.web.dto.LeadSummaryDTO;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import com.screenleads.backend.app.web.dto.PromotionLeadDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionsController {

    @Autowired
    private PromotionService promotionService;

    // ===== CRUD existente =====
    @GetMapping
    public List<PromotionDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/{id}")
    public PromotionDTO getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id);
    }

    @PostMapping
    public PromotionDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.savePromotion(promotionDTO);
    }

    @PutMapping("/{id}")
    public PromotionDTO updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }

    // ===== Leads =====
    @PostMapping("/{id}/leads")
    public PromotionLeadDTO registerLead(@PathVariable Long id, @RequestBody PromotionLeadDTO leadDTO) {
        return promotionService.registerLead(id, leadDTO);
    }

    @GetMapping("/{id}/leads")
    public List<PromotionLeadDTO> listLeads(@PathVariable Long id) {
        return promotionService.listLeads(id);
    }

    // ===== Export CSV (Streaming) =====
    @GetMapping(value = "/{id}/leads/export.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportLeadsCsv(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        // Por defecto: últimos 30 días
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
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
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
        ZonedDateTime fromZdt = parseZdtOrDefault(from, toZdt.minusDays(30));
        return promotionService.getLeadSummary(id, fromZdt, toZdt);
    }

    private ZonedDateTime parseZdtOrDefault(String s, ZonedDateTime defaultValue) {
        if (s == null || s.isBlank())
            return defaultValue;
        try {
            // Acepta "2025-09-01" (asume 00:00 Europe/Madrid) o ISO completo
            // "2025-09-01T10:30:00+02:00"
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return d.atStartOfDay(ZoneId.of("Europe/Madrid"));
            }
            return ZonedDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }
}

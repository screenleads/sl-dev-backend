package com.screenleads.backend.app.web.controller;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AdviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;

@RestController
@RequestMapping("/advices")
public class AdvicesController {

    private static final Logger logger = LoggerFactory.getLogger(AdvicesController.class);

    private final AdviceService adviceService;

    public AdvicesController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @GetMapping
    public ResponseEntity<List<AdviceDTO>> getAllAdvices() {
        return ResponseEntity.ok(adviceService.getAllAdvices());
    }

    /**
     * Devuelve los anuncios visibles "ahora" según la zona horaria del cliente,
     * leída de los headers:
     * - X-Timezone: IANA TZ (p.ej. "Europe/Madrid")
     * - X-Timezone-Offset: minutos al ESTE de UTC (p.ej. "120")
     */
    @GetMapping("/visibles")
    public ResponseEntity<List<AdviceDTO>> getVisibleAdvicesNow(
            @RequestHeader(value = "X-Timezone", required = false) String tz,
            @RequestHeader(value = "X-Timezone-Offset", required = false) String offsetMinutesStr) {

        ZoneId zone = resolveZoneId(tz, offsetMinutesStr);
        logger.debug("Resolviendo visibles con zona: {}", zone);

        return ResponseEntity.ok(adviceService.getVisibleAdvicesNow(zone));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdviceDTO> getAdviceById(@PathVariable Long id) {
        Optional<AdviceDTO> advice = adviceService.getAdviceById(id);
        return advice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AdviceDTO> createAdvice(@RequestBody AdviceDTO adviceDTO) {
        return ResponseEntity.ok(adviceService.saveAdvice(adviceDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdviceDTO> updateAdvice(@PathVariable Long id, @RequestBody AdviceDTO adviceDTO) {
        logger.info("adviceDTO object: {}", adviceDTO);
        AdviceDTO updatedAdvice = adviceService.updateAdvice(id, adviceDTO);
        return ResponseEntity.ok(updatedAdvice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvice(@PathVariable Long id) {
        adviceService.deleteAdvice(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------- helpers -----------------

    private ZoneId resolveZoneId(String tz, String offsetMinutesStr) {
        if (tz != null && !tz.isBlank()) {
            try {
                return ZoneId.of(tz.trim());
            } catch (Exception ignored) {
            }
        }
        if (offsetMinutesStr != null && !offsetMinutesStr.isBlank()) {
            try {
                int minutes = Integer.parseInt(offsetMinutesStr.trim());
                return ZoneOffset.ofTotalSeconds(minutes * 60);
            } catch (Exception ignored) {
            }
        }
        return ZoneId.systemDefault();
    }
}

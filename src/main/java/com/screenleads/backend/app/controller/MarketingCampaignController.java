package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.AudienceSegmentRepository;
import com.screenleads.backend.app.domain.repository.NotificationTemplateRepository;
import com.screenleads.backend.app.dto.request.MarketingCampaignRequest;
import com.screenleads.backend.app.service.MarketingCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing-campaigns")
@RequiredArgsConstructor
@Slf4j
public class MarketingCampaignController {

    private final MarketingCampaignService campaignService;
    private final AudienceSegmentRepository audienceSegmentRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;

    /**
     * Crea una nueva campaña de marketing
     */
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','create')")
    public ResponseEntity<MarketingCampaign> createCampaign(
            @Valid @RequestBody MarketingCampaignRequest request,
            @RequestParam Long companyId) {

        AudienceSegment segment = audienceSegmentRepository.findById(request.getAudienceSegmentId())
                .orElseThrow(
                        () -> new RuntimeException("Audience segment not found: " + request.getAudienceSegmentId()));

        NotificationTemplate template = notificationTemplateRepository.findById(request.getNotificationTemplateId())
                .orElseThrow(() -> new RuntimeException(
                        "Notification template not found: " + request.getNotificationTemplateId()));

        Company company = new Company();
        company.setId(companyId);

        MarketingCampaign campaign = MarketingCampaign.builder()
                .company(company)
                .name(request.getName())
                .description(request.getDescription())
                .audienceSegment(segment)
                .notificationTemplate(template)
                .scheduledAt(request.getScheduledAt())
                .metadata(request.getMetadata())
                .build();

        MarketingCampaign created = campaignService.createCampaign(campaign);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza una campaña existente
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','update')")
    public ResponseEntity<MarketingCampaign> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody MarketingCampaignRequest request) {

        AudienceSegment segment = audienceSegmentRepository.findById(request.getAudienceSegmentId())
                .orElseThrow(
                        () -> new RuntimeException("Audience segment not found: " + request.getAudienceSegmentId()));

        NotificationTemplate template = notificationTemplateRepository.findById(request.getNotificationTemplateId())
                .orElseThrow(() -> new RuntimeException(
                        "Notification template not found: " + request.getNotificationTemplateId()));

        MarketingCampaign campaign = MarketingCampaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .audienceSegment(segment)
                .notificationTemplate(template)
                .metadata(request.getMetadata())
                .build();

        MarketingCampaign updated = campaignService.updateCampaign(id, campaign);

        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina una campaña (solo DRAFT)
     */
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','delete')")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene una campaña por ID
     */
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<MarketingCampaign> getCampaign(@PathVariable Long id) {
        return campaignService.getCampaignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene todas las campañas de una compañía
     */
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<MarketingCampaign>> getCampaignsByCompany(@RequestParam Long companyId) {
        List<MarketingCampaign> campaigns = campaignService.getCampaignsByCompany(companyId);
        return ResponseEntity.ok(campaigns);
    }

    /**
     * Obtiene campañas por estado
     */
    @GetMapping("/status/{status}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<MarketingCampaign>> getCampaignsByStatus(
            @PathVariable CampaignStatus status,
            @RequestParam Long companyId) {
        List<MarketingCampaign> campaigns = campaignService.getCampaignsByStatus(companyId, status);
        return ResponseEntity.ok(campaigns);
    }

    /**
     * Programa una campaña para ejecución futura
     */
    @PostMapping("/{id}/schedule")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingCampaign> scheduleCampaign(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        LocalDateTime scheduledAt = LocalDateTime.parse(request.get("scheduledAt"));
        MarketingCampaign scheduled = campaignService.scheduleCampaign(id, scheduledAt);

        return ResponseEntity.ok(scheduled);
    }

    /**
     * Ejecuta una campaña inmediatamente
     */
    @PostMapping("/{id}/execute")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingCampaign> executeCampaign(@PathVariable Long id) {
        MarketingCampaign executed = campaignService.executeCampaign(id);
        return ResponseEntity.ok(executed);
    }

    /**
     * Pausa una campaña en ejecución
     */
    @PostMapping("/{id}/pause")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingCampaign> pauseCampaign(@PathVariable Long id) {
        MarketingCampaign paused = campaignService.pauseCampaign(id);
        return ResponseEntity.ok(paused);
    }

    /**
     * Resume una campaña pausada
     */
    @PostMapping("/{id}/resume")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingCampaign> resumeCampaign(@PathVariable Long id) {
        MarketingCampaign resumed = campaignService.resumeCampaign(id);
        return ResponseEntity.ok(resumed);
    }

    /**
     * Cancela una campaña
     */
    @PostMapping("/{id}/cancel")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingCampaign> cancelCampaign(@PathVariable Long id) {
        MarketingCampaign cancelled = campaignService.cancelCampaign(id);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Obtiene estadísticas de una campaña
     */
    @GetMapping("/{id}/statistics")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<Map<String, Object>> getCampaignStatistics(@PathVariable Long id) {
        MarketingCampaign campaign = campaignService.getCampaignStatistics(id);

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("campaignId", campaign.getId());
        stats.put("name", campaign.getName());
        stats.put("status", campaign.getStatus());
        stats.put("targetAudienceSize", campaign.getTargetAudienceSize());
        stats.put("sentCount", campaign.getSentCount());
        stats.put("successCount", campaign.getSuccessCount());
        stats.put("failedCount", campaign.getFailedCount());
        stats.put("openCount", campaign.getOpenCount());
        stats.put("clickCount", campaign.getClickCount());
        stats.put("successRate", campaign.getSuccessRate());
        stats.put("openRate", campaign.getOpenRate());
        stats.put("clickRate", campaign.getClickRate());

        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene las campañas más exitosas
     */
    @GetMapping("/top")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<MarketingCampaign>> getTopCampaigns(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "10") int limit) {
        List<MarketingCampaign> topCampaigns = campaignService.getTopCampaigns(companyId, limit);
        return ResponseEntity.ok(topCampaigns);
    }

    /**
     * Job programado para ejecutar campañas pendientes
     * Se ejecuta cada 5 minutos
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processPendingCampaigns() {
        log.info("Starting scheduled job to process pending campaigns");
        try {
            campaignService.processPendingCampaigns();
        } catch (Exception e) {
            log.error("Error in scheduled campaign processing: {}", e.getMessage(), e);
        }
    }
}

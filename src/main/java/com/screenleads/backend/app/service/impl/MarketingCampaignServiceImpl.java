package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.MarketingCampaignRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import com.screenleads.backend.app.service.MarketingCampaignService;
import com.screenleads.backend.app.service.NotificationService;
import com.screenleads.backend.app.service.NotificationTemplateService;
import com.screenleads.backend.app.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketingCampaignServiceImpl implements MarketingCampaignService {

    private final MarketingCampaignRepository campaignRepository;
    private final AudienceSegmentService audienceSegmentService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public MarketingCampaign createCampaign(MarketingCampaign campaign) {
        log.info("Creating new marketing campaign: {} for company ID: {}",
                campaign.getName(), campaign.getCompany().getId());

        // Calcular tamaño de audiencia objetivo
        Long audienceSize = audienceSegmentService.countCustomersInSegment(campaign.getAudienceSegment().getId());
        campaign.setTargetAudienceSize(audienceSize);

        campaign.setStatus(CampaignStatus.DRAFT);

        MarketingCampaign saved = campaignRepository.save(campaign);

        log.info("Campaign created with ID: {} targeting {} customers", saved.getId(), audienceSize);
        return saved;
    }

    @Override
    @Transactional
    public MarketingCampaign updateCampaign(Long campaignId, MarketingCampaign campaign) {
        MarketingCampaign existing = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        // Solo permitir actualización si está en DRAFT o SCHEDULED
        if (existing.isFinalized()) {
            throw new RuntimeException("Cannot update finalized campaign: " + campaignId);
        }

        existing.setName(campaign.getName());
        existing.setDescription(campaign.getDescription());

        // Si cambia el segmento, recalcular audiencia
        if (!existing.getAudienceSegment().getId().equals(campaign.getAudienceSegment().getId())) {
            existing.setAudienceSegment(campaign.getAudienceSegment());
            Long audienceSize = audienceSegmentService.countCustomersInSegment(campaign.getAudienceSegment().getId());
            existing.setTargetAudienceSize(audienceSize);
        }

        if (!existing.getNotificationTemplate().getId().equals(campaign.getNotificationTemplate().getId())) {
            existing.setNotificationTemplate(campaign.getNotificationTemplate());
        }

        existing.setMetadata(campaign.getMetadata());

        return campaignRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        // Solo permitir eliminación si está en DRAFT
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new RuntimeException("Can only delete campaigns in DRAFT status");
        }

        campaignRepository.delete(campaign);
        log.info("Campaign deleted: {}", campaignId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketingCampaign> getCampaignById(Long campaignId) {
        return campaignRepository.findById(campaignId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketingCampaign> getCampaignsByCompany(Long companyId) {
        return campaignRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketingCampaign> getCampaignsByStatus(Long companyId, CampaignStatus status) {
        return campaignRepository.findByCompany_IdAndStatusOrderByCreatedAtDesc(companyId, status);
    }

    @Override
    @Transactional
    public MarketingCampaign scheduleCampaign(Long campaignId, LocalDateTime scheduledAt) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new RuntimeException("Can only schedule campaigns in DRAFT status");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot schedule campaign in the past");
        }

        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus(CampaignStatus.SCHEDULED);

        MarketingCampaign saved = campaignRepository.save(campaign);

        log.info("Campaign {} scheduled for execution at {}", campaignId, scheduledAt);
        return saved;
    }

    @Override
    @Transactional
    public MarketingCampaign executeCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        if (campaign.isFinalized()) {
            throw new RuntimeException("Campaign already finalized: " + campaignId);
        }

        log.info("Starting execution of campaign: {} ({})", campaign.getId(), campaign.getName());

        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartedAt(LocalDateTime.now());
        campaign.setExecutionError(null);

        campaignRepository.save(campaign);

        try {
            // Obtener clientes del segmento
            List<Customer> customers = audienceSegmentService
                    .getCustomersInSegment(campaign.getAudienceSegment().getId());

            log.info("Campaign {} targeting {} customers", campaignId, customers.size());

            campaign.setTargetAudienceSize((long) customers.size());

            long successCount = 0;
            long failedCount = 0;

            // Renderizar template
            NotificationTemplate template = campaign.getNotificationTemplate();

            // Enviar notificaciones (simulación - en producción integrar con servicio real)
            for (Customer customer : customers) {
                try {
                    // Preparar variables para el template
                    var variables = new java.util.HashMap<String, String>();
                    variables.put("customerName",
                            customer.getFullName() != null ? customer.getFullName() : customer.getEmail());
                    variables.put("email", customer.getEmail());
                    variables.put("companyName", campaign.getCompany().getName());
                    variables.put("campaignName", campaign.getName());

                    // Determinar recipient según el canal
                    String recipient = null;
                    NotificationChannel channel = template.getChannel();
                    switch (channel) {
                        case EMAIL:
                            recipient = customer.getEmail();
                            break;
                        case SMS:
                            recipient = customer.getPhone();
                            break;
                        case PUSH_NOTIFICATION:
                            // TODO: Get device token from customer
                            log.warn("Push notifications not yet implemented for customer {}", customer.getId());
                            continue;
                        default:
                            log.warn("Unsupported channel {} for customer {}", channel, customer.getId());
                            continue;
                    }

                    if (recipient == null || recipient.isEmpty()) {
                        log.warn("No recipient found for customer {} with channel {}", customer.getId(), channel);
                        failedCount++;
                        continue;
                    }

                    // Enviar notificación usando el nuevo NotificationService
                    NotificationResponse response = notificationService.sendFromTemplate(
                            template.getId(),
                            recipient,
                            variables);

                    if (response.isSuccess()) {
                        log.debug("Sent {} notification to {}: {}", channel, recipient, response.getNotificationId());
                        successCount++;
                    } else {
                        log.error("Failed to send {} notification to {}: {}", channel, recipient,
                                response.getErrorMessage());
                        failedCount++;
                    }

                } catch (Exception e) {
                    log.error("Failed to send notification to customer {}: {}", customer.getId(), e.getMessage());
                    failedCount++;
                }
            }

            campaign.setSentCount((long) customers.size());
            campaign.setSuccessCount(successCount);
            campaign.setFailedCount(failedCount);
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setCompletedAt(LocalDateTime.now());

            // Incrementar contador de uso del template
            notificationTemplateService.incrementUsageCount(template.getId());

            log.info("Campaign {} completed: {} sent, {} successful, {} failed",
                    campaignId, customers.size(), successCount, failedCount);

        } catch (Exception e) {
            log.error("Error executing campaign {}: {}", campaignId, e.getMessage(), e);
            campaign.setStatus(CampaignStatus.FAILED);
            campaign.setExecutionError(e.getMessage());
            campaign.setCompletedAt(LocalDateTime.now());
        }

        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public MarketingCampaign pauseCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.RUNNING) {
            throw new RuntimeException("Can only pause running campaigns");
        }

        campaign.setStatus(CampaignStatus.PAUSED);

        log.info("Campaign {} paused", campaignId);
        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public MarketingCampaign resumeCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new RuntimeException("Can only resume paused campaigns");
        }

        campaign.setStatus(CampaignStatus.RUNNING);

        log.info("Campaign {} resumed", campaignId);
        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public MarketingCampaign cancelCampaign(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));

        if (campaign.isFinalized()) {
            throw new RuntimeException("Campaign already finalized");
        }

        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign.setCompletedAt(LocalDateTime.now());

        log.info("Campaign {} cancelled", campaignId);
        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketingCampaign getCampaignStatistics(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
    }

    @Override
    @Transactional
    public void processPendingCampaigns() {
        log.info("Processing pending scheduled campaigns");

        List<MarketingCampaign> pendingCampaigns = campaignRepository.findPendingCampaigns(LocalDateTime.now());

        log.info("Found {} campaigns ready for execution", pendingCampaigns.size());

        for (MarketingCampaign campaign : pendingCampaigns) {
            try {
                log.info("Auto-executing scheduled campaign: {} ({})", campaign.getId(), campaign.getName());
                executeCampaign(campaign.getId());
            } catch (Exception e) {
                log.error("Error auto-executing campaign {}: {}", campaign.getId(), e.getMessage());
            }
        }

        log.info("Finished processing pending campaigns");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketingCampaign> getTopCampaigns(Long companyId, int limit) {
        return campaignRepository.findTopCampaignsByOpenRate(companyId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

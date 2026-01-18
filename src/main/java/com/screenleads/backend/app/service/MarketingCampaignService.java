package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.CampaignStatus;
import com.screenleads.backend.app.domain.model.MarketingCampaign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketingCampaignService {

    /**
     * Crea una nueva campaña de marketing
     */
    MarketingCampaign createCampaign(MarketingCampaign campaign);

    /**
     * Actualiza una campaña existente
     */
    MarketingCampaign updateCampaign(Long campaignId, MarketingCampaign campaign);

    /**
     * Elimina una campaña (solo si está en DRAFT)
     */
    void deleteCampaign(Long campaignId);

    /**
     * Obtiene una campaña por ID
     */
    Optional<MarketingCampaign> getCampaignById(Long campaignId);

    /**
     * Obtiene todas las campañas de una compañía
     */
    List<MarketingCampaign> getCampaignsByCompany(Long companyId);

    /**
     * Obtiene campañas por estado
     */
    List<MarketingCampaign> getCampaignsByStatus(Long companyId, CampaignStatus status);

    /**
     * Programa una campaña para ejecución futura
     */
    MarketingCampaign scheduleCampaign(Long campaignId, LocalDateTime scheduledAt);

    /**
     * Ejecuta una campaña inmediatamente
     */
    MarketingCampaign executeCampaign(Long campaignId);

    /**
     * Pausa una campaña en ejecución
     */
    MarketingCampaign pauseCampaign(Long campaignId);

    /**
     * Resume una campaña pausada
     */
    MarketingCampaign resumeCampaign(Long campaignId);

    /**
     * Cancela una campaña programada o en ejecución
     */
    MarketingCampaign cancelCampaign(Long campaignId);

    /**
     * Obtiene estadísticas de una campaña
     */
    MarketingCampaign getCampaignStatistics(Long campaignId);

    /**
     * Procesa campañas pendientes programadas (ejecutado por scheduler)
     */
    void processPendingCampaigns();

    /**
     * Obtiene las campañas más exitosas
     */
    List<MarketingCampaign> getTopCampaigns(Long companyId, int limit);
}

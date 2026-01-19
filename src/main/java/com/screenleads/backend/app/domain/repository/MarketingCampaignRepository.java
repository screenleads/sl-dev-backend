package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.CampaignStatus;
import com.screenleads.backend.app.domain.model.MarketingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Long> {

    /**
     * Encuentra todas las campañas de una compañía
     */
    List<MarketingCampaign> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    /**
     * Encuentra campañas por estado
     */
    List<MarketingCampaign> findByCompany_IdAndStatusOrderByCreatedAtDesc(Long companyId, CampaignStatus status);

    /**
     * Encuentra campañas programadas pendientes de ejecución
     */
    @Query("SELECT c FROM MarketingCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledAt <= :now")
    List<MarketingCampaign> findPendingCampaigns(@Param("now") LocalDateTime now);

    /**
     * Encuentra campañas activas (en ejecución)
     */
    @Query("SELECT c FROM MarketingCampaign c WHERE c.status = 'RUNNING'")
    List<MarketingCampaign> findActiveCampaigns();

    /**
     * Encuentra campañas completadas en un rango de fechas
     */
    @Query("SELECT c FROM MarketingCampaign c WHERE c.company.id = :companyId AND c.status = 'COMPLETED' AND c.completedAt BETWEEN :startDate AND :endDate")
    List<MarketingCampaign> findCompletedCampaignsBetween(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta campañas por estado para una compañía
     */
    Long countByCompany_IdAndStatus(Long companyId, CampaignStatus status);

    /**
     * Encuentra campañas asociadas a un segmento de audiencia
     */
    List<MarketingCampaign> findByAudienceSegment_IdOrderByCreatedAtDesc(Long segmentId);

    /**
     * Encuentra campañas asociadas a una plantilla de notificación
     */
    List<MarketingCampaign> findByNotificationTemplate_IdOrderByCreatedAtDesc(Long templateId);

    /**
     * Verifica si existe una campaña activa para un segmento
     */
    @Query("SELECT COUNT(c) > 0 FROM MarketingCampaign c WHERE c.audienceSegment.id = :segmentId AND c.status IN ('RUNNING', 'SCHEDULED')")
    boolean existsActiveCampaignForSegment(@Param("segmentId") Long segmentId);

    /**
     * Encuentra las campañas más exitosas (por tasa de apertura)
     */
    @Query("SELECT c FROM MarketingCampaign c WHERE c.company.id = :companyId AND c.status = 'COMPLETED' ORDER BY c.openCount DESC")
    List<MarketingCampaign> findTopCampaignsByOpenRate(@Param("companyId") Long companyId);
}

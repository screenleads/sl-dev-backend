package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * Find all templates for a specific company
     */
    List<NotificationTemplate> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    /**
     * Find active templates for a company
     */
    List<NotificationTemplate> findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(Long companyId);

    /**
     * Find templates by channel for a company
     */
    List<NotificationTemplate> findByCompany_IdAndChannelOrderByCreatedAtDesc(Long companyId, NotificationChannel channel);

    /**
     * Find active templates by channel for a company
     */
    List<NotificationTemplate> findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
        Long companyId, NotificationChannel channel);

    /**
     * Find template by name and company
     */
    Optional<NotificationTemplate> findByNameAndCompany_Id(String name, Long companyId);

    /**
     * Check if template name exists for a company
     */
    boolean existsByNameAndCompany_Id(String name, Long companyId);

    /**
     * Find most used templates for a company
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.company.id = :companyId " +
           "AND nt.isActive = true ORDER BY nt.usageCount DESC, nt.lastUsedAt DESC")
    List<NotificationTemplate> findMostUsedByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find templates created by a specific user
     */
    List<NotificationTemplate> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Count active templates by channel for a company
     */
    Long countByCompany_IdAndChannelAndIsActiveTrue(Long companyId, NotificationChannel channel);
}

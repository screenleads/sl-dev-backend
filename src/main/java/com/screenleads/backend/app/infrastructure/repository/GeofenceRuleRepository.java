package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.GeofenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeofenceRuleRepository extends JpaRepository<GeofenceRule, Long> {

    List<GeofenceRule> findByPromotion_Id(Long promotionId);

    List<GeofenceRule> findByZone_Id(Long zoneId);

    List<GeofenceRule> findByIsActiveTrue();

    @Query("SELECT gr FROM GeofenceRule gr " +
           "JOIN FETCH gr.zone gz " +
           "JOIN FETCH gr.promotion p " +
           "WHERE p.company.id = :companyId " +
           "AND gr.isActive = true " +
           "AND gz.isActive = true " +
           "ORDER BY gr.priority DESC")
    List<GeofenceRule> findActiveRulesByCompany(@Param("companyId") Long companyId);

    @Query("SELECT gr FROM GeofenceRule gr " +
           "JOIN FETCH gr.zone gz " +
           "JOIN FETCH gr.promotion p " +
           "WHERE p.id = :promotionId " +
           "AND gr.isActive = true " +
           "ORDER BY gr.priority DESC")
    List<GeofenceRule> findActiveRulesByPromotion(@Param("promotionId") Long promotionId);
}

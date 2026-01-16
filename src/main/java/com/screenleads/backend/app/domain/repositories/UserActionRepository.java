package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.UserAction;
import com.screenleads.backend.app.domain.model.UserActionType;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    
    List<UserAction> findByCustomerId(Long customerId);
    
    List<UserAction> findByCustomerIdOrderByTimestampDesc(Long customerId);
    
    List<UserAction> findByDeviceId(Long deviceId);
    
    List<UserAction> findByActionType(UserActionType actionType);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.customer.id = :customerId AND ua.actionType = :actionType")
    List<UserAction> findByCustomerAndActionType(@Param("customerId") Long customerId, @Param("actionType") UserActionType actionType);
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.customer.id = :customerId AND ua.timestamp BETWEEN :startDate AND :endDate ORDER BY ua.timestamp DESC")
    List<UserAction> findByCustomerAndDateRange(
        @Param("customerId") Long customerId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
    
    @Query("SELECT ua FROM UserAction ua WHERE ua.entityType = :entityType AND ua.entityId = :entityId ORDER BY ua.timestamp DESC")
    List<UserAction> findByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    @Query("SELECT COUNT(ua) FROM UserAction ua WHERE ua.customer.id = :customerId AND ua.actionType = :actionType")
    Long countByCustomerAndActionType(@Param("customerId") Long customerId, @Param("actionType") UserActionType actionType);
}

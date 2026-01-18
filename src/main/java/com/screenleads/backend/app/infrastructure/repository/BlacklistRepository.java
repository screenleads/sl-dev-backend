package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.Blacklist;
import com.screenleads.backend.app.domain.model.BlacklistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    Optional<Blacklist> findByCompany_IdAndBlacklistTypeAndValueAndIsActiveTrue(
        Long companyId, 
        BlacklistType blacklistType, 
        String value
    );

    List<Blacklist> findByCompany_IdAndBlacklistType(Long companyId, BlacklistType blacklistType);

    List<Blacklist> findByCompany_IdAndIsActiveTrue(Long companyId);

    @Query("SELECT b FROM Blacklist b WHERE b.company.id = :companyId " +
           "AND b.blacklistType = :type " +
           "AND b.value = :value " +
           "AND b.isActive = true " +
           "AND (b.expiresAt IS NULL OR b.expiresAt > :now)")
    Optional<Blacklist> findEffectiveBlacklist(
        @Param("companyId") Long companyId,
        @Param("type") BlacklistType type,
        @Param("value") String value,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT b FROM Blacklist b WHERE b.isActive = true " +
           "AND b.expiresAt IS NOT NULL " +
           "AND b.expiresAt <= :now")
    List<Blacklist> findExpiredBlacklists(@Param("now") LocalDateTime now);

    long countByCompany_IdAndIsActiveTrue(Long companyId);
}

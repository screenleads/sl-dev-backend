package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.InvitationStatus;
import com.screenleads.backend.app.domain.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {

    Optional<UserInvitation> findByToken(String token);

    Optional<UserInvitation> findByEmailAndStatus(String email, InvitationStatus status);

    List<UserInvitation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<UserInvitation> findByStatusAndExpiryDateBefore(InvitationStatus status, LocalDateTime date);

    boolean existsByEmailAndStatusIn(String email, List<InvitationStatus> statuses);

    @Modifying
    @Query("UPDATE UserInvitation i SET i.status = 'EXPIRED' WHERE i.status = 'PENDING' AND i.expiryDate < :now")
    int markExpiredInvitations(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(i) FROM UserInvitation i WHERE i.company.id = :companyId AND i.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") InvitationStatus status);
}

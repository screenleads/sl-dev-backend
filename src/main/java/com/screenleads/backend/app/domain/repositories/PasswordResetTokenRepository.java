// src/main/java/com/screenleads/backend/app/domain/repositories/PasswordResetTokenRepository.java
package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.PasswordResetToken;
import com.screenleads.backend.app.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Buscar token por su valor
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Buscar tokens por usuario (ordenados por fecha de creación descendente)
     */
    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);

    /**
     * Eliminar tokens expirados (limpieza periódica)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Marcar todos los tokens de un usuario como usados
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void markAllUserTokensAsUsed(@Param("user") User user);

    /**
     * Verificar si existe un token válido (no usado y no expirado) para un usuario
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM PasswordResetToken t " +
            "WHERE t.user = :user AND t.used = false AND t.expiryDate > :now")
    boolean hasValidToken(@Param("user") User user, @Param("now") LocalDateTime now);
}

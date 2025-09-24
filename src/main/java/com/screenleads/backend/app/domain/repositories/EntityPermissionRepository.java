// src/main/java/com/screenleads/backend/app/domain/repositories/EntityPermissionRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.screenleads.backend.app.domain.model.EntityPermission;

public interface EntityPermissionRepository extends JpaRepository<EntityPermission, Long> {
    Optional<EntityPermission> findByResource(String resource);
}

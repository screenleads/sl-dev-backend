// src/main/java/.../domain/repositories/AppEntityAttributeRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;

public interface AppEntityAttributeRepository extends JpaRepository<AppEntityAttribute, Long> {
    List<AppEntityAttribute> findByAppEntityIdOrderByListOrderAscIdAsc(Long appEntityId);

    Optional<AppEntityAttribute> findByAppEntityIdAndName(Long appEntityId, String name);
}

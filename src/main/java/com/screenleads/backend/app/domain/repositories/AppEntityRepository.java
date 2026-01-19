package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AppEntity;

public interface AppEntityRepository extends JpaRepository<AppEntity, Long> {

    Optional<AppEntity> findByResource(String resource);

    Optional<AppEntity> findByEndpointBase(String endpointBase);

    // Para reordenamiento de menú
    List<AppEntity> findByVisibleInMenuTrueOrderBySortOrderAsc();

    // Para cargar atributos al reordenar
    @EntityGraph(attributePaths = "attributes")
    Optional<AppEntity> findWithAttributesById(Long id);
}

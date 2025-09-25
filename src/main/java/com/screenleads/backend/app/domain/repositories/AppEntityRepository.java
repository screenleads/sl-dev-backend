package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AppEntity;

public interface AppEntityRepository extends JpaRepository<AppEntity, Long> {
    Optional<AppEntity> findByResource(String resource);
    Optional<AppEntity> findByEndpointBase(String endpointBase);
    boolean existsByResource(String resource);
    boolean existsByEndpointBase(String endpointBase);
}

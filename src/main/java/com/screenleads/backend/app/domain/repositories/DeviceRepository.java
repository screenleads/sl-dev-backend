package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Device;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByUuid(String uuid);

    Device findByUuid(String uuid); // ya lo usabas

    Optional<Device> findOptionalByUuid(String uuid); // para 404 limpio
}
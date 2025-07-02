package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Device;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByUuid(String uuid);

    Device findByUuid(String uuid);
}
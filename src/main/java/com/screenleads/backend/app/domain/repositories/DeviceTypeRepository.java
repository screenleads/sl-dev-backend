package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.DeviceType;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {
    Optional<DeviceType> findByType(String type);

}
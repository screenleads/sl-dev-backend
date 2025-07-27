package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AppVersion;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findTopByPlatformOrderByIdDesc(String platform);
}
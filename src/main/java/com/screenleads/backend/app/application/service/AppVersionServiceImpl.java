package com.screenleads.backend.app.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.AppVersion;
import com.screenleads.backend.app.domain.repositories.AppVersionRepository;
import com.screenleads.backend.app.web.dto.AppVersionDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionRepository repository;

    @Override
    public AppVersionDTO save(AppVersionDTO dto) {
        AppVersion entity = toEntity(dto);
        AppVersion saved = repository.save(entity);
        return toDTO(saved);
    }

    @Override
    public List<AppVersionDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public AppVersionDTO findById(Long id) {
        AppVersion entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AppVersion not found with id " + id));
        return toDTO(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public AppVersionDTO getLatestVersion(String platform) {
        AppVersion entity = repository.findTopByPlatformOrderByIdDesc(platform)
                .orElseThrow(() -> new RuntimeException("No version found for platform " + platform));
        return toDTO(entity);
    }

    // --- Métodos de conversión ---
    private AppVersionDTO toDTO(AppVersion entity) {
        return AppVersionDTO.builder()
                .id(entity.getId())
                .platform(entity.getPlatform())
                .version(entity.getVersion())
                .message(entity.getMessage())
                .url(entity.getUrl())
                .forceUpdate(entity.isForceUpdate())
                .build();
    }

    private AppVersion toEntity(AppVersionDTO dto) {
        return AppVersion.builder()
                .id(dto.getId())
                .platform(dto.getPlatform())
                .version(dto.getVersion())
                .message(dto.getMessage())
                .url(dto.getUrl())
                .forceUpdate(dto.isForceUpdate())
                .build();
    }
}

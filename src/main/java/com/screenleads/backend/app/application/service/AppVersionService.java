package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.web.dto.AppVersionDTO;

public interface AppVersionService {
    AppVersionDTO save(AppVersionDTO dto);

    List<AppVersionDTO> findAll();

    AppVersionDTO findById(Long id);

    void deleteById(Long id);

    AppVersionDTO getLatestVersion(String platform);
}
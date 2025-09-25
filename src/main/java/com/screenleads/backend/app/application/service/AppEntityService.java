package com.screenleads.backend.app.application.service;
import java.util.List;

import com.screenleads.backend.app.web.dto.AppEntityDTO;

public interface AppEntityService {

    List<AppEntityDTO> findAll(boolean withCount);

    AppEntityDTO findById(Long id, boolean withCount);

    AppEntityDTO findByResource(String resource, boolean withCount);

    AppEntityDTO upsert(AppEntityDTO dto);

    void deleteById(Long id);
}

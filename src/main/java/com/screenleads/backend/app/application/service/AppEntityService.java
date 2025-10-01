package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.web.dto.AppEntityDTO;

public interface AppEntityService {

    // ===== Query =====
    List<AppEntityDTO> findAll(boolean withCount);

    AppEntityDTO findById(Long id, boolean withCount);

    AppEntityDTO findByResource(String resource, boolean withCount);

    // ===== Commands =====
    AppEntityDTO upsert(AppEntityDTO dto);

    void deleteById(Long id);

    // ===== Reorder (drag & drop) =====
    /**
     * Reordena las entidades visibles en menú (visibleInMenu=true) con base en
     * la lista de IDs recibida. Resequia sortOrder empezando en 1.
     */
    void reorderEntities(List<Long> orderedIds);

    /**
     * Reordena los atributos de una entidad por la lista de IDs recibida.
     * Actualiza listOrder y, si procede, formOrder para mantener consistencia.
     */
    void reorderAttributes(Long entityId, List<Long> orderedAttributeIds);
}

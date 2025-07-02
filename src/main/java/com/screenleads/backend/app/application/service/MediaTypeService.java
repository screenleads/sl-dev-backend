package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.MediaTypeDTO;

public interface MediaTypeService {
    List<MediaTypeDTO> getAllMediaTypes();

    Optional<MediaTypeDTO> getMediaTypeById(Long id);

    MediaTypeDTO saveMediaType(MediaTypeDTO MediaTypeDTO);

    MediaTypeDTO updateMediaType(Long id, MediaTypeDTO MediaTypeDTO);

    void deleteMediaType(Long id);
}

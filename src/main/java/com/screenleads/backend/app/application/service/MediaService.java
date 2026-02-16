package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.MediaDTO;

public interface MediaService {
    List<MediaDTO> getAllMedias();

    Optional<MediaDTO> getMediaById(Long id);

    MediaDTO saveMedia(MediaDTO dto);

    MediaDTO updateMedia(Long id, MediaDTO dto);

    void deleteMedia(Long id);

    MediaDTO saveMediaFromUpload(String url, String type);
}

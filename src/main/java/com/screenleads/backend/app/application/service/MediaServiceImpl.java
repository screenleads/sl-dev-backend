package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.MediaDTO;

@Service
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaTypeRepository mediaTypeRepository;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaTypeRepository mediaTypeRepository) {
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    @Override
    public List<MediaDTO> getAllMedias() {
        return mediaRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(MediaDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MediaDTO> getMediaById(Long id) {
        return mediaRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public MediaDTO saveMedia(MediaDTO mediaDTO) {
        Media media = convertToEntity(mediaDTO);
        Media savedMedia = mediaRepository.save(media);
        return convertToDTO(savedMedia);
    }

    @Override
    public MediaDTO updateMedia(Long id, MediaDTO mediaDTO) {
        Media media = mediaRepository.findById(id).orElseThrow();
        media.setSrc(mediaDTO.src());
        media.setType(mediaDTO.type());
        Media updatedMedia = mediaRepository.save(media);
        return convertToDTO(updatedMedia);
    }

    @Override
    public void deleteMedia(Long id) {
        mediaRepository.deleteById(id);
    }

    // Convert Media Entity to MediaDTO
    private MediaDTO convertToDTO(Media media) {
        return new MediaDTO(media.getId(), media.getSrc(), media.getType());
    }

    // Convert MediaDTO to Media Entity
    private Media convertToEntity(MediaDTO mediaDTO) {
        Media media = new Media();
        media.setId(mediaDTO.id());
        media.setSrc(mediaDTO.src());
        if (mediaDTO.type() == null || mediaDTO.type().getId() == null) {
            throw new IllegalArgumentException("Media type requerido");
        }
        media.setType(mediaTypeRepository.findById(mediaDTO.type().getId())
                .orElseThrow(() -> new IllegalArgumentException("Media type no encontrado")));
        // TODO: recibir o inferir companyId; sin eso, se caerá el save si es NOT NULL
        return media;
    }

}

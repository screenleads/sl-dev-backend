package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@Service
public class MediaTypeServiceImpl implements MediaTypeService {

    private static final String MEDIA_TYPE_NOT_FOUND_WITH_ID = "MediaType not found with id: ";

    private final MediaTypeRepository mediaTypeRepository;

    public MediaTypeServiceImpl(MediaTypeRepository mediaTypeRepository) {
        this.mediaTypeRepository = mediaTypeRepository;
    }

    @Override
    public List<MediaTypeDTO> getAllMediaTypes() {
        return mediaTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(MediaTypeDTO::id))
                .toList();
    }

    @Override
    public Optional<MediaTypeDTO> getMediaTypeById(Long id) {
        return mediaTypeRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public MediaTypeDTO saveMediaType(MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = convertToEntity(mediaTypeDTO);
        Optional<MediaType> existedByExtension = mediaTypeRepository.findByExtension(mediaType.getExtension());
        Optional<MediaType> existedByType = mediaTypeRepository.findByType(mediaType.getType());
        if (existedByExtension.isPresent())
            return convertToDTO(existedByExtension.get());
        if (existedByType.isPresent())
            return convertToDTO(existedByType.get());
        MediaType savedMediaType = mediaTypeRepository.save(mediaType);
        return convertToDTO(savedMediaType);
    }

    @Override
    public MediaTypeDTO updateMediaType(Long id, MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = mediaTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MEDIA_TYPE_NOT_FOUND_WITH_ID + id));
        mediaType.setEnabled(mediaTypeDTO.enabled());
        mediaType.setExtension(mediaTypeDTO.extension());
        mediaType.setType(mediaTypeDTO.type());
        MediaType updatedMediaType = mediaTypeRepository.save(mediaType);
        return convertToDTO(updatedMediaType);
    }

    @Override
    public void deleteMediaType(Long id) {
        mediaTypeRepository.deleteById(id);
    }

    // Convert MediaType Entity to MediaTypeDTO
    private MediaTypeDTO convertToDTO(MediaType mediaType) {
        return new MediaTypeDTO(mediaType.getId(), mediaType.getExtension(), mediaType.getType(),
                mediaType.getEnabled());
    }

    // Convert MediaTypeDTO to MediaType Entity
    private MediaType convertToEntity(MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = new MediaType();
        mediaType.setId(mediaTypeDTO.id());
        mediaType.setExtension(mediaTypeDTO.extension());
        mediaType.setType(mediaTypeDTO.type());
        mediaType.setEnabled(mediaTypeDTO.enabled());
        return mediaType;
    }
}

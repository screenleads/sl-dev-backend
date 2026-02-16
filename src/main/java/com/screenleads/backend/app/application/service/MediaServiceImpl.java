package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.MediaDTO;

@Service
public class MediaServiceImpl implements MediaService {

    private static final String MEDIA_TYPE_REQUIRED = "Media type requerido";
    private static final String MEDIA_TYPE_NOT_FOUND = "Media type no encontrado";
    private static final String MEDIA_NOT_FOUND_WITH_ID = "Media not found with id: ";

    private final MediaRepository mediaRepository;
    private final MediaTypeRepository mediaTypeRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaTypeRepository mediaTypeRepository,
            UserRepository userRepository, CompanyRepository companyRepository) {
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<MediaDTO> getAllMedias() {
        return mediaRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(MediaDTO::id))
                .toList();
    }

    @Override
    public List<com.screenleads.backend.app.domain.model.MediaType> getAllMediaTypes() {
        return mediaTypeRepository.findAll();
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
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MEDIA_NOT_FOUND_WITH_ID + id));
        media.setSrc(mediaDTO.src());
        media.setType(mediaDTO.type());
        Media updatedMedia = mediaRepository.save(media);
        return convertToDTO(updatedMedia);
    }

    @Override
    public void deleteMedia(Long id) {
        mediaRepository.deleteById(id);
    }
    
    @Override
    public MediaDTO saveMediaFromUpload(String url, String type) {
        // Determinar el MediaType ID basado en el tipo de archivo
        Long mediaTypeId = determineMediaTypeId(type);
        
        // Buscar el MediaType en la BD
        var mediaType = mediaTypeRepository.findById(mediaTypeId)
                .orElseThrow(() -> new IllegalArgumentException("MediaType not found with id: " + mediaTypeId));
        
        // Obtener la compañía del usuario autenticado
        Long companyId = getCurrentCompanyId();
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + companyId));
        }
        
        // Crear y guardar la Media
        Media media = new Media();
        media.setSrc(url);
        media.setType(mediaType);
        media.setCompany(company);
        
        Media savedMedia = mediaRepository.save(media);
        return convertToDTO(savedMedia);
    }
    
    private Long determineMediaTypeId(String type) {
        // Mapear el tipo de archivo al ID del MediaType en la BD
        // Esto puede variar según tu configuración de MediaTypes
        return switch (type.toLowerCase()) {
            case "image" -> 1L; // Asumiendo que 1 es imagen
            case "video" -> 2L; // Asumiendo que 2 es video
            default -> 1L; // Por defecto imagen
        };
    }
    
    private Long getCurrentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return resolveCompanyId(auth);
    }
    
    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof com.screenleads.backend.app.domain.model.User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        return null;
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
            throw new IllegalArgumentException(MEDIA_TYPE_REQUIRED);
        }
        media.setType(mediaTypeRepository.findById(mediaDTO.type().getId())
                .orElseThrow(() -> new IllegalArgumentException(MEDIA_TYPE_NOT_FOUND)));
        return media;
    }

}

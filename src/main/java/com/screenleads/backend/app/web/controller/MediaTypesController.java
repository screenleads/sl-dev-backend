package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

import com.screenleads.backend.app.application.service.MediaTypeService;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@RestController
@RequiredArgsConstructor
public class MediaTypesController {

    private final MediaTypeService mediaTypeService;

    @CrossOrigin
    @GetMapping("/medias/types")
    @PreAuthorize("@perm.can('media_type', 'read')")
    public ResponseEntity<List<MediaTypeDTO>> getAllMediaTypes() {
        return ResponseEntity.ok(mediaTypeService.getAllMediaTypes());
    }

    @CrossOrigin
    @GetMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('media_type', 'read')")
    public ResponseEntity<MediaTypeDTO> getMediaTypeById(@PathVariable Long id) {
        Optional<MediaTypeDTO> device = mediaTypeService.getMediaTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias/types")
    @PreAuthorize("@perm.can('media_type', 'write')")
    public ResponseEntity<MediaTypeDTO> createMediaType(@RequestBody MediaTypeDTO deviceDTO) {
        return ResponseEntity.ok(mediaTypeService.saveMediaType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('media_type', 'write')")
    public ResponseEntity<MediaTypeDTO> updateMediaType(@PathVariable Long id, @RequestBody MediaTypeDTO deviceDTO) {

        MediaTypeDTO updatedDevice = mediaTypeService.updateMediaType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

    }

    @CrossOrigin
    @DeleteMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('media_type', 'delete')")
    public ResponseEntity<Void> deleteMediaType(@PathVariable Long id) {
        mediaTypeService.deleteMediaType(id);
        return ResponseEntity.ok().build();
    }
}
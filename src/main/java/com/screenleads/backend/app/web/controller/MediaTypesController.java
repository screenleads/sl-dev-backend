package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
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

@Controller
@RequiredArgsConstructor
public class MediaTypesController {

    private final MediaTypeService deviceTypeService;

    @CrossOrigin
    @GetMapping("/medias/types")
    @PreAuthorize("@perm.can('mediatype', 'read')")
    public ResponseEntity<List<MediaTypeDTO>> getAllMediaTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllMediaTypes());
    }

    @CrossOrigin
    @GetMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'read')")
    public ResponseEntity<MediaTypeDTO> getMediaTypeById(@PathVariable Long id) {
        Optional<MediaTypeDTO> device = deviceTypeService.getMediaTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias/types")
    @PreAuthorize("@perm.can('mediatype', 'create')")
    public ResponseEntity<MediaTypeDTO> createMediaType(@RequestBody MediaTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveMediaType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'update')")
    public ResponseEntity<MediaTypeDTO> updateMediaType(@PathVariable Long id, @RequestBody MediaTypeDTO deviceDTO) {

        MediaTypeDTO updatedDevice = deviceTypeService.updateMediaType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

    }

    @CrossOrigin
    @DeleteMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'delete')")
    public ResponseEntity<String> deleteMediaType(@PathVariable Long id) {
        deviceTypeService.deleteMediaType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}
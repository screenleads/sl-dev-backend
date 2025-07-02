package com.screenleads.backend.app.web.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.screenleads.backend.app.application.service.MediaService;
import com.screenleads.backend.app.web.dto.MediaDTO;

import java.util.List;
import java.util.Optional;

@Controller
public class MediaController {
    // Endpoint to serve image file

    @Autowired
    private MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @CrossOrigin
    @GetMapping("/medias")
    public ResponseEntity<List<MediaDTO>> getAllMedias() {
        return ResponseEntity.ok(mediaService.getAllMedias());
    }

    @CrossOrigin
    @GetMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable Long id) {
        Optional<MediaDTO> device = mediaService.getMediaById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias")
    public ResponseEntity<MediaDTO> createMedia(@RequestBody MediaDTO deviceDTO) {
        return ResponseEntity.ok(mediaService.saveMedia(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> updateMedia(@PathVariable Long id, @RequestBody MediaDTO deviceDTO) {
        try {
            MediaDTO updatedDevice = mediaService.updateMedia(id, deviceDTO);
            return ResponseEntity.ok(updatedDevice);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @CrossOrigin
    @DeleteMapping("/medias/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }

    @CrossOrigin
    @GetMapping("/medias/render/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws Exception {

        try {
            Optional<MediaDTO> mediaaux = mediaService.getMediaById(id);
            // Ruta absoluta o relativa donde se encuentran los archivos
            Path filePath = Paths.get("src/main/resources/static/medias/").resolve(mediaaux.get().src()).normalize();
            System.out.println(filePath.toString());

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determinar tipo de contenido (puedes mejorarlo con Files.probeContentType)
            String contentType = "application/octet-stream"; // Por defecto

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
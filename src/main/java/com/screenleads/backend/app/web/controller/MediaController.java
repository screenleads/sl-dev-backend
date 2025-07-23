package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.FirebaseStorageService;
import com.screenleads.backend.app.application.service.MediaService;
import com.screenleads.backend.app.web.dto.MediaDTO;
import jakarta.servlet.annotation.MultipartConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@Slf4j
public class MediaController {

    @Autowired
    private FirebaseStorageService firebaseService;

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
    @PostMapping("/medias/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("🚀 Iniciando proceso de subida...");

        // 1. Guardar archivo temporalmente
        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);
        log.info("📁 Archivo recibido: {}", tempFile.getAbsolutePath());

        // 2. Subir a carpeta "raw/" del bucket
        String filename = UUID.randomUUID() + ".mp4";
        String firebasePath = "raw/" + filename;
        String publicUrl = firebaseService.upload(tempFile, firebasePath);
        log.info("📤 Archivo subido a Firebase Storage (raw): {}", publicUrl);

        // 3. Limpiar archivo temporal
        tempFile.delete();

        return ResponseEntity.ok(Map.of("url", publicUrl));
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
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @GetMapping("/medias/render/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws Exception {
        try {
            Optional<MediaDTO> mediaaux = mediaService.getMediaById(id);
            Path filePath = Paths.get("src/main/resources/static/medias/").resolve(mediaaux.get().src()).normalize();
            log.info("📂 Cargando archivo desde: {}", filePath.toString());

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

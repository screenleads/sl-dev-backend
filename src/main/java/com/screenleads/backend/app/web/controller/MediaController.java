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

        // 1. Subir archivo a carpeta temporal 'raw/' en Firebase
        String originalFileName = file.getOriginalFilename();
        String rawPath = "raw/" + UUID.randomUUID() + "-" + originalFileName;

        File tempFile = File.createTempFile("upload-", originalFileName);
        file.transferTo(tempFile);

        firebaseService.upload(tempFile, rawPath);
        log.info("📤 Archivo subido a Firebase en {}", rawPath);

        // 2. Polling para esperar a que aparezca el archivo comprimido
        String compressedPath = rawPath.replace("raw/", "media/").replaceAll("\\.(mp4|mov|webm)$", "_compressed.mp4");

        int maxAttempts = 20;
        int waitMs = 10000;
        boolean exists = false;

        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(waitMs);
            if (firebaseService.exists(compressedPath)) {
                exists = true;
                break;
            }
            log.info("⌛ Esperando archivo comprimido... intento {}/{}", i + 1, maxAttempts);
        }

        // 3. Resultado
        if (exists) {
            String publicUrl = firebaseService.getPublicUrl(compressedPath);
            log.info("✅ Archivo comprimido disponible en {}", publicUrl);
            return ResponseEntity.ok(Map.of("url", publicUrl));
        } else {
            log.warn("⏱️ Timeout esperando archivo comprimido.");
            return ResponseEntity.status(504).body(Map.of("error", "Timeout esperando la compresión"));
        }
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

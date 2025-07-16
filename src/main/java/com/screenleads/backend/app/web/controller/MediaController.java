package com.screenleads.backend.app.web.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.screenleads.backend.app.application.service.FirebaseStorageService;
import com.screenleads.backend.app.application.service.MediaService;
import com.screenleads.backend.app.web.dto.MediaDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Map;

import jakarta.servlet.annotation.MultipartConfig;
import lombok.extern.slf4j.Slf4j;

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
        File tempInput = File.createTempFile("input-", file.getOriginalFilename());
        file.transferTo(tempInput);
        log.info("📁 Archivo recibido y guardado: {}", tempInput.getAbsolutePath());

        String filename = file.getOriginalFilename().toLowerCase();
        boolean isVideo = filename.endsWith(".mp4") || filename.endsWith(".mov") || filename.endsWith(".webm");

        File output = isVideo ? File.createTempFile("compressed-", ".mp4") : tempInput;

        if (isVideo) {
            log.info("📹 Tipo de archivo: Video - se comprimirá");

            ProcessBuilder builder = new ProcessBuilder(
                    "C:\\ProgramData\\chocolatey\\bin\\ffmpeg.exe",
                    "-y",
                    "-i", tempInput.getAbsolutePath(),
                    "-vf", "scale=1080:-2",
                    "-c:v", "libx264",
                    "-crf", "28",
                    "-preset", "slow",
                    "-an", // <-- elimina el audio
                    output.getAbsolutePath());

            builder.redirectErrorStream(true);
            log.info("⚙️ Ejecutando ffmpeg...");
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("ffmpeg >> {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("✅ ffmpeg finalizó correctamente");
            } else {
                log.warn("⚠️ ffmpeg terminó con código {}", exitCode);
                throw new RuntimeException("La compresión falló con código " + exitCode);
            }
        }

        // 3. Subir a Firebase
        String storagePath = "media/" + UUID.randomUUID() + "-" + output.getName();
        String publicUrl = firebaseService.upload(output, storagePath);
        log.info("📤 Archivo subido a Firebase con URL: {}", publicUrl);

        // 4. Limpiar archivos temporales
        tempInput.delete();
        if (!output.equals(tempInput))
            output.delete();
        log.info("🧹 Archivos temporales eliminados");

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
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
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

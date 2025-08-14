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

        // 1. Subir archivo a 'raw/'
        String originalFileName = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "-" + originalFileName;
        String rawPath = "raw/" + fileName;

        File tempFile = File.createTempFile("upload-", originalFileName);
        file.transferTo(tempFile);

        firebaseService.upload(tempFile, rawPath);
        log.info("📤 Archivo subido a Firebase en {}", rawPath);

        // 2. Responder sincrónicamente con el nombre del archivo para posterior
        // consulta
        return ResponseEntity.accepted().body(Map.of("filename", fileName));
    }

    @CrossOrigin
    @GetMapping("/medias/status/{filename}")
    public ResponseEntity<Map<String, Object>> checkCompressionStatus(@PathVariable String filename) {
        log.info("📡 Comprobando estado de compresión para: {}", filename);

        final String IMG_DEST = "media/images";
        final String VID_DEST = "media/videos";
        final int[] IMAGE_THUMBS = { 320, 640 };
        final int[] VIDEO_THUMBS = { 320, 640 };

        String base = stripExtension(filename);
        MediaKind kind = detectKind(filename);
        log.debug("🔍 Tipo detectado: {} | Base name: {}", kind, base);

        // 1) Compatibilidad retro
        String legacyPath = "media/compressed-" + filename;
        log.debug("🔍 Revisando ruta legacy: {}", legacyPath);
        if (firebaseService.exists(legacyPath)) {
            String url = firebaseService.getPublicUrl(legacyPath);
            log.info("✅ Archivo comprimido disponible (legacy): {}", url);
            return ResponseEntity.ok(Map.of(
                    "status", "ready",
                    "type", "legacy",
                    "url", url,
                    "thumbnails", List.of()));
        }

        // 2) Nuevas rutas
        List<String> mainCandidates = new ArrayList<>();
        List<String> thumbCandidates = new ArrayList<>();

        if (kind == MediaKind.VIDEO) {
            String main = VID_DEST + "/compressed-" + base + ".mp4";
            mainCandidates.add(main);
            for (int s : VIDEO_THUMBS) {
                thumbCandidates.add(VID_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
            }
        } else if (kind == MediaKind.IMAGE) {
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".jpg");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".png");
            for (int s : IMAGE_THUMBS) {
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".png");
            }
        } else {
            log.warn("⚠️ Tipo de archivo desconocido, probando rutas genéricas");
            mainCandidates.add(VID_DEST + "/compressed-" + base + ".mp4");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".jpg");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".png");
            int[] sizes = { 320, 640 };
            for (int s : sizes) {
                thumbCandidates.add(VID_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".png");
            }
        }

        // 3) Buscar el main
        String foundMain = null;
        for (String cand : mainCandidates) {
            log.debug("🔍 Revisando main candidate: {}", cand);
            if (firebaseService.exists(cand)) {
                foundMain = cand;
                break;
            }
        }

        if (foundMain != null) {
            log.info("✅ Archivo principal encontrado: {}", foundMain);
            List<String> thumbs = new ArrayList<>();
            for (String t : thumbCandidates) {
                if (firebaseService.exists(t)) {
                    log.debug("🖼️ Thumbnail encontrado: {}", t);
                    thumbs.add(firebaseService.getPublicUrl(t));
                }
            }

            String type = foundMain.startsWith(VID_DEST) ? "video"
                    : foundMain.startsWith(IMG_DEST) ? "image"
                            : "unknown";

            String publicUrl = firebaseService.getPublicUrl(foundMain);
            log.info("📤 URL pública: {}", publicUrl);
            return ResponseEntity.ok(Map.of(
                    "status", "ready",
                    "type", type,
                    "url", publicUrl,
                    "thumbnails", thumbs));
        } else {
            log.info("🕓 Archivo aún no está comprimido");
            return ResponseEntity.status(202).body(Map.of("status", "processing"));
        }
    }

    private enum MediaKind {
        VIDEO, IMAGE, UNKNOWN
    }

    private MediaKind detectKind(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".mp4") || f.endsWith(".mov") || f.endsWith(".webm"))
            return MediaKind.VIDEO;
        if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".webp")
                || f.endsWith(".avif") || f.endsWith(".heic") || f.endsWith(".heif") || f.endsWith(".gif"))
            return MediaKind.IMAGE;
        return MediaKind.UNKNOWN;
    }

    private String stripExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(0, i) : filename;
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

        MediaDTO updatedDevice = mediaService.updateMedia(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

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

    }
}

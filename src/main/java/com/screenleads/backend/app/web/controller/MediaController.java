package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.FirebaseStorageService;
import com.screenleads.backend.app.application.service.MediaProcessingService;
import com.screenleads.backend.app.application.service.MediaService;
import com.screenleads.backend.app.web.dto.MediaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.*;

@RestController // <-- antes era @Controller
@Slf4j
public class MediaController {

    private static final String ERROR_KEY = "error";
    private static final String STATUS_KEY = "status";
    private static final String COMPRESSED_PREFIX = "/compressed-";
    private static final String THUMB_PREFIX = "/thumb-";
    private static final String THUMBNAILS_PATH = "/thumbnails/";

    private final FirebaseStorageService firebaseService;
    private final MediaService mediaService;
    private final MediaProcessingService processingService;

    public MediaController(FirebaseStorageService firebaseService, MediaService mediaService,
            MediaProcessingService processingService) {
        this.firebaseService = firebaseService;
        this.mediaService = mediaService;
        this.processingService = processingService;
    }

    // ---------------- LIST/CRUD ----------------

    @PreAuthorize("@perm.can('media', 'read')")
    @CrossOrigin
    @GetMapping("/medias")
    public ResponseEntity<List<MediaDTO>> getAllMedias() {
        return ResponseEntity.ok(mediaService.getAllMedias());
    }

    // ---------------- UPLOAD (SÍNCRONO) ----------------

    @PreAuthorize("@perm.can('media', 'create')")
    @CrossOrigin
    @PostMapping(value = "/medias/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Archivo vacío"));
            }

            final String original = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
            final String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
            final String fileName = UUID.randomUUID() + "-" + safeName;

            // Crear archivo temporal
            Path tmpDir = Paths.get(Optional.ofNullable(System.getProperty("java.io.tmpdir")).orElse("/tmp"));
            Files.createDirectories(tmpDir);
            Path tmp = Files.createTempFile(tmpDir, "upload_", "_" + safeName);

            log.info("📥 Recibido multipart: name={}, size={} bytes, contentType={}",
                    safeName, file.getSize(), file.getContentType());

            // Copiar a temporal
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            // Procesar de forma síncrona (comprimir + thumbnails + subir)
            log.info("🔄 Iniciando procesamiento síncrono...");
            MediaProcessingService.ProcessedMedia result = processingService.processMedia(tmp.toFile(), fileName,
                    firebaseService);

            // Guardar Media en la base de datos
            MediaDTO savedMedia = mediaService.saveMediaFromUpload(result.mainUrl(), result.type());
            
            // Limpiar temporal
            deleteTempFile(tmp);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ Proceso completo en {}ms, Media ID: {}", totalTime, savedMedia.id());

            // Retornar resultado con el ID de la Media guardada
            return ResponseEntity.ok(Map.of(
                    "id", savedMedia.id(),
                    "src", savedMedia.src(),
                    "type", result.type(),
                    STATUS_KEY, "ready",
                    "url", result.mainUrl(),
                    "thumbnails", result.thumbnailUrls(),
                    "processingTimeMs", totalTime));

        } catch (MaxUploadSizeExceededException tooBig) {
            return ResponseEntity.status(413).body(Map.of(ERROR_KEY, "Archivo demasiado grande"));
        } catch (Exception ex) {
            log.error("❌ Error procesando archivo", ex);
            return ResponseEntity.status(500).body(Map.of(
                    ERROR_KEY, "Fallo procesando archivo",
                    "detail", ex.getMessage()));
        }
    }

    // ---------------- UPLOAD FROM URL (SÍNCRONO) ----------------

    @PreAuthorize("@perm.can('media', 'create')")
    @CrossOrigin
    @PostMapping(value = "/medias/upload-from-url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFromUrl(@RequestBody Map<String, String> request) {
        long startTime = System.currentTimeMillis();

        try {
            String fileUrl = request.get("url");
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "URL vacía o inválida"));
            }

            log.info("🌐 Recibida URL para procesar: {}", fileUrl);

            // Descargar archivo desde URL
            URL url = new URI(fileUrl).toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000); // 10 segundos timeout
            connection.setReadTimeout(30000); // 30 segundos timeout

            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();

            log.info("📥 Descargando archivo: size={} bytes, contentType={}",
                    contentLength > 0 ? contentLength : "unknown", contentType);

            // Extraer nombre del archivo de la URL
            String urlPath = url.getPath();
            String originalName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            if (originalName.isEmpty() || !originalName.contains(".")) {
                originalName = "download-" + System.currentTimeMillis() + ".bin";
            }

            final String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
            final String fileName = UUID.randomUUID() + "-" + safeName;

            // Crear archivo temporal
            Path tmpDir = Paths.get(Optional.ofNullable(System.getProperty("java.io.tmpdir")).orElse("/tmp"));
            Files.createDirectories(tmpDir);
            Path tmp = Files.createTempFile(tmpDir, "url_download_", "_" + safeName);

            // Descargar a temporal
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            long downloadedSize = Files.size(tmp);
            log.info("✅ Archivo descargado: {} bytes", downloadedSize);

            // Procesar de forma síncrona (comprimir + thumbnails + subir)
            log.info("🔄 Iniciando procesamiento síncrono...");
            MediaProcessingService.ProcessedMedia result = processingService.processMedia(tmp.toFile(), fileName,
                    firebaseService);

            // Limpiar temporal
            deleteTempFile(tmp);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ Proceso completo en {}ms", totalTime);

            // Retornar resultado inmediato (sin polling)
            return ResponseEntity.ok(Map.of(
                    STATUS_KEY, "ready",
                    "type", result.type(),
                    "url", result.mainUrl(),
                    "thumbnails", result.thumbnailUrls(),
                    "processingTimeMs", totalTime));

        } catch (Exception ex) {
            log.error("❌ Error procesando archivo desde URL", ex);
            return ResponseEntity.status(500).body(Map.of(
                    ERROR_KEY, "Fallo procesando archivo desde URL",
                    "detail", ex.getMessage()));
        }
    }

    // ---------------- CRUD restantes (sin tocar rutas) ----------------

    @CrossOrigin
    @GetMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable Long id) {
        return mediaService.getMediaById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias")
    public ResponseEntity<MediaDTO> createMedia(@RequestBody MediaDTO deviceDTO) {
        return ResponseEntity.ok(mediaService.saveMedia(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> updateMedia(@PathVariable Long id, @RequestBody MediaDTO deviceDTO) {
        return ResponseEntity.ok(mediaService.updateMedia(id, deviceDTO));
    }

    @CrossOrigin
    @DeleteMapping("/medias/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @GetMapping("/medias/render/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws MediaException {
        try {
            Optional<MediaDTO> mediaaux = mediaService.getMediaById(id);
            if (!mediaaux.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get("src/main/resources/static/medias/").resolve(mediaaux.get().src()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists())
                return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new MediaException("Failed to render media", e);
        }
    }

    private void deleteTempFile(Path tmp) {
        try {
            Files.deleteIfExists(tmp);
        } catch (Exception e) {
            log.warn("No se pudo eliminar archivo temporal {}: {}", tmp, e.getMessage());
        }
    }
}

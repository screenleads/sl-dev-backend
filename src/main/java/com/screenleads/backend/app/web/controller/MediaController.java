package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.FirebaseStorageService;
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

    public MediaController(FirebaseStorageService firebaseService, MediaService mediaService) {
        this.firebaseService = firebaseService;
        this.mediaService = mediaService;
    }

    // ---------------- LIST/CRUD ----------------

    @PreAuthorize("@perm.can('media', 'read')")
    @CrossOrigin
    @GetMapping("/medias")
    public ResponseEntity<List<MediaDTO>> getAllMedias() {
        return ResponseEntity.ok(mediaService.getAllMedias());
    }

    // ---------------- UPLOAD ----------------

    @PreAuthorize("@perm.can('media', 'create')")
    @CrossOrigin
    @PostMapping(value = "/medias/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Archivo vacío"));
            }

            final String original = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
            final String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
            final String fileName = UUID.randomUUID() + "-" + safeName;
            final String rawPath = "raw/" + fileName;

            // /tmp es el disco efímero de Heroku
            Path tmpDir = Paths.get(Optional.ofNullable(System.getProperty("java.io.tmpdir")).orElse("/tmp"));
            Files.createDirectories(tmpDir);
            Path tmp = Files.createTempFile(tmpDir, "upload_", "_" + safeName);

            log.info("📥 Recibido multipart: name={}, size={} bytes, contentType={}",
                    safeName, file.getSize(), file.getContentType());

            // copiar a tmp
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            // subir a Firebase (raw/)
            firebaseService.upload(tmp.toFile(), rawPath);
            log.info("📤 Subido a Firebase RAW: {}", rawPath);

            // opcional: borrar temporal
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception e) {
                log.warn("No se pudo eliminar archivo temporal {}: {}", tmp, e.getMessage());
            }

            // 200 OK con filename (el front hace polling a /medias/status/{filename})
            return ResponseEntity.ok(Map.of("filename", fileName));

        } catch (MaxUploadSizeExceededException tooBig) {
            return ResponseEntity.status(413).body(Map.of(ERROR_KEY, "Archivo demasiado grande"));
        } catch (Exception ex) {
            log.error("❌ Error subiendo archivo", ex);
            return ResponseEntity.status(500).body(Map.of(
                    ERROR_KEY, "Fallo subiendo archivo",
                    "detail", ex.getMessage()));
        }
    }

    @CrossOrigin
    @GetMapping(value = "/medias/status/{filename}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> checkCompressionStatus(@PathVariable String filename) {
        log.info("📡 Comprobando estado de compresión: {}", filename);

        String base = stripExtension(filename);
        MediaKind kind = detectKind(filename);

        // 1) compatibilidad legacy
        ResponseEntity<Map<String, Object>> legacyResponse = checkLegacyPath(filename);
        if (legacyResponse != null) {
            return legacyResponse;
        }

        // 2) rutas nuevas
        CandidatePaths paths = buildCandidatePaths(base, kind);
        String foundMain = findFirstExisting(paths.mainCandidates);

        if (foundMain != null) {
            return buildSuccessResponse(foundMain, paths.thumbCandidates);
        }

        return ResponseEntity.status(202).body(Map.of(STATUS_KEY, "processing"));
    }

    private ResponseEntity<Map<String, Object>> checkLegacyPath(String filename) {
        String legacyPath = "media" + COMPRESSED_PREFIX + filename;
        if (firebaseService.exists(legacyPath)) {
            return ResponseEntity.ok(Map.of(
                    STATUS_KEY, "ready",
                    "type", "legacy",
                    "url", firebaseService.getPublicUrl(legacyPath),
                    "thumbnails", List.of()));
        }
        return null;
    }

    private CandidatePaths buildCandidatePaths(String base, MediaKind kind) {
        final String IMG_DEST = "media/images";
        final String VID_DEST = "media/videos";
        final int[] IMAGE_THUMBS = { 320, 640 };
        final int[] VIDEO_THUMBS = { 320, 640 };

        List<String> mainCandidates = new ArrayList<>();
        List<String> thumbCandidates = new ArrayList<>();

        if (kind == MediaKind.VIDEO) {
            mainCandidates.add(VID_DEST + COMPRESSED_PREFIX + base + ".mp4");
            for (int s : VIDEO_THUMBS) {
                thumbCandidates.add(VID_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".jpg");
            }
        } else if (kind == MediaKind.IMAGE) {
            mainCandidates.add(IMG_DEST + COMPRESSED_PREFIX + base + ".jpg");
            mainCandidates.add(IMG_DEST + COMPRESSED_PREFIX + base + ".png");
            for (int s : IMAGE_THUMBS) {
                thumbCandidates.add(IMG_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".png");
            }
        } else {
            mainCandidates.add(VID_DEST + COMPRESSED_PREFIX + base + ".mp4");
            mainCandidates.add(IMG_DEST + COMPRESSED_PREFIX + base + ".jpg");
            mainCandidates.add(IMG_DEST + COMPRESSED_PREFIX + base + ".png");
            for (int s : new int[] { 320, 640 }) {
                thumbCandidates.add(VID_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + THUMBNAILS_PATH + s + THUMB_PREFIX + s + "-" + base + ".png");
            }
        }

        return new CandidatePaths(mainCandidates, thumbCandidates);
    }

    private String findFirstExisting(List<String> candidates) {
        for (String cand : candidates) {
            if (firebaseService.exists(cand)) {
                return cand;
            }
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String foundMain, List<String> thumbCandidates) {
        List<String> thumbs = thumbCandidates.stream()
                .filter(firebaseService::exists)
                .map(firebaseService::getPublicUrl)
                .toList();

        String type = foundMain.startsWith("media/videos") ? "video"
                : foundMain.startsWith("media/images") ? "image" : "unknown";

        return ResponseEntity.ok(Map.of(
                STATUS_KEY, "ready",
                "type", type,
                "url", firebaseService.getPublicUrl(foundMain),
                "thumbnails", thumbs));
    }

    private record CandidatePaths(List<String> mainCandidates, List<String> thumbCandidates) {
    }

    private enum MediaKind {
        VIDEO, IMAGE, UNKNOWN
    }

    private MediaKind detectKind(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".mp4") || f.endsWith(".mov") || f.endsWith(".webm"))
            return MediaKind.VIDEO;
        if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png")
                || f.endsWith(".webp") || f.endsWith(".avif") || f.endsWith(".heic")
                || f.endsWith(".heif") || f.endsWith(".gif"))
            return MediaKind.IMAGE;
        return MediaKind.UNKNOWN;
    }

    private String stripExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(0, i) : filename;
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
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws Exception {
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
    }
}

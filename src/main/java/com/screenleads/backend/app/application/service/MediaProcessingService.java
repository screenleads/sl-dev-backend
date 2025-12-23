package com.screenleads.backend.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class MediaProcessingService {

    private static final int[] THUMBNAIL_SIZES = { 320, 640 };
    private static final int VIDEO_BITRATE = 1000000; // 1Mbps
    private static final int AUDIO_BITRATE = 128000; // 128kbps
    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;
    
    /**
     * Custom ProcessLocator que usa el FFmpeg de Heroku buildpack
     */
    private static class HerokuFFmpegLocator implements ProcessLocator {
        private final String ffmpegPath;
        
        public HerokuFFmpegLocator(String ffmpegPath) {
            this.ffmpegPath = ffmpegPath;
        }
        
        @Override
        public String getExecutablePath() {
            return ffmpegPath;
        }
    }
    
    /**
     * Obtiene el ProcessLocator correcto para FFmpeg.
     * En Heroku, usa el FFmpeg instalado por el buildpack en /app/vendor/ffmpeg/ffmpeg
     * En local, usa el FFmpeg embebido de JAVE
     */
    private ProcessLocator getFFmpegLocator() {
        // Verificar si estamos en Heroku (buildpack instala FFmpeg en /app/vendor/ffmpeg/)
        String herokuFFmpegPath = "/app/vendor/ffmpeg/ffmpeg";
        File herokuFFmpeg = new File(herokuFFmpegPath);
        
        if (herokuFFmpeg.exists() && herokuFFmpeg.canExecute()) {
            log.info("🎬 Usando FFmpeg de Heroku buildpack: {}", herokuFFmpegPath);
            log.info("📍 Verificando permisos: readable={}, writable={}, executable={}",
                    herokuFFmpeg.canRead(), herokuFFmpeg.canWrite(), herokuFFmpeg.canExecute());
            return new HerokuFFmpegLocator(herokuFFmpegPath);
        }
        
        log.info("💻 Usando FFmpeg embebido de JAVE (ambiente local)");
        return new DefaultFFMPEGLocator(); // Default = JAVE usará su FFmpeg embebido
    }

    public record ProcessingResult(
            String mainUrl,
            List<String> thumbnailUrls,
            String type,
            long processingTimeMs) {
    }

    /**
     * Procesa un archivo multimedia de forma síncrona: comprime y genera thumbnails
     */
    public ProcessedMedia processMedia(File sourceFile, String originalFilename,
            FirebaseStorageService firebaseService) throws IOException {
        long startTime = System.currentTimeMillis();

        String extension = getExtension(originalFilename).toLowerCase(Locale.ROOT);
        MediaType mediaType = detectMediaType(extension);

        log.info("🎬 Iniciando procesamiento {} de archivo: {}", mediaType, originalFilename);

        ProcessedMedia result;

        try {
            if (mediaType == MediaType.VIDEO) {
                result = processVideo(sourceFile, originalFilename, firebaseService);
            } else if (mediaType == MediaType.IMAGE) {
                result = processImage(sourceFile, originalFilename, firebaseService);
            } else {
                throw new UnsupportedOperationException("Tipo de archivo no soportado: " + extension);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Procesamiento completado en {}ms", processingTime);

            return result;

        } catch (Exception e) {
            log.error("❌ Error procesando media: {}", e.getMessage(), e);
            throw new IOException("Error procesando archivo: " + e.getMessage(), e);
        }
    }

    private ProcessedMedia processVideo(File sourceFile, String originalFilename,
            FirebaseStorageService firebaseService) throws IOException {
        String baseName = stripExtension(originalFilename);
        String destinationFolder = "media/videos";

        // 1. Comprimir video
        File compressedVideo = compressVideo(sourceFile);
        String compressedPath = destinationFolder + "/compressed-" + baseName + ".mp4";
        String mainUrl = firebaseService.upload(compressedVideo, compressedPath);
        log.info("📤 Video comprimido subido: {}", compressedPath);

        // 2. Generar thumbnails del video
        List<String> thumbnailUrls = new ArrayList<>();
        for (int size : THUMBNAIL_SIZES) {
            try {
                File thumbnail = extractVideoThumbnail(sourceFile, size);
                String thumbPath = String.format("%s/thumbnails/%d/thumb-%d-%s.jpg",
                        destinationFolder, size, size, baseName);
                String thumbUrl = firebaseService.upload(thumbnail, thumbPath);
                thumbnailUrls.add(thumbUrl);
                Files.deleteIfExists(thumbnail.toPath());
            } catch (Exception e) {
                log.warn("⚠️ No se pudo generar thumbnail de {}px: {}", size, e.getMessage());
            }
        }

        Files.deleteIfExists(compressedVideo.toPath());

        return new ProcessedMedia(mainUrl, thumbnailUrls, "video");
    }

    private ProcessedMedia processImage(File sourceFile, String originalFilename,
            FirebaseStorageService firebaseService) throws IOException {
        String baseName = stripExtension(originalFilename);
        String extension = getExtension(originalFilename).toLowerCase(Locale.ROOT);
        String destinationFolder = "media/images";

        // 1. Comprimir/redimensionar imagen
        File compressedImage = compressImage(sourceFile, extension);
        String compressedPath = destinationFolder + "/compressed-" + baseName + "." + extension;
        String mainUrl = firebaseService.upload(compressedImage, compressedPath);
        log.info("📤 Imagen comprimida subida: {}", compressedPath);

        // 2. Generar thumbnails
        List<String> thumbnailUrls = new ArrayList<>();
        for (int size : THUMBNAIL_SIZES) {
            try {
                File thumbnail = createImageThumbnail(sourceFile, size);
                String thumbPath = String.format("%s/thumbnails/%d/thumb-%d-%s.jpg",
                        destinationFolder, size, size, baseName);
                String thumbUrl = firebaseService.upload(thumbnail, thumbPath);
                thumbnailUrls.add(thumbUrl);
                Files.deleteIfExists(thumbnail.toPath());
            } catch (Exception e) {
                log.warn("⚠️ No se pudo generar thumbnail de {}px: {}", size, e.getMessage());
            }
        }

        Files.deleteIfExists(compressedImage.toPath());

        return new ProcessedMedia(mainUrl, thumbnailUrls, "image");
    }

    private File compressVideo(File source) throws IOException {
        try {
            Path tempOutput = Files.createTempFile("compressed_video_", ".mp4");
            File target = tempOutput.toFile();

            // Obtener FFmpeg locator ANTES de crear MultimediaObject
            ProcessLocator ffmpegLocator = getFFmpegLocator();
            
            MultimediaObject multimediaObject = new MultimediaObject(source, ffmpegLocator);
            MultimediaInfo info = multimediaObject.getInfo();

            // Configurar atributos de audio
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("aac");
            audio.setBitRate(AUDIO_BITRATE);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            // Configurar atributos de video con opciones compatibles para Heroku
            VideoAttributes video = new VideoAttributes();
            video.setCodec("libx264"); // Usar libx264 explícitamente
            video.setBitRate(VIDEO_BITRATE);
            video.setFrameRate(30);

            // Mantener aspect ratio pero limitar resolución
            VideoInfo videoInfo = info.getVideo();
            if (videoInfo != null) {
                int width = videoInfo.getSize().getWidth();
                int height = videoInfo.getSize().getHeight();
                log.info("📐 Resolución original: {}x{}", width, height);

                if (width > 1920 || height > 1080) {
                    double scale = Math.min(1920.0 / width, 1080.0 / height);
                    int newWidth = (int) (width * scale);
                    int newHeight = (int) (height * scale);
                    // Asegurar que las dimensiones sean pares (requerido por h264)
                    newWidth = (newWidth / 2) * 2;
                    newHeight = (newHeight / 2) * 2;
                    video.setSize(new ws.schild.jave.info.VideoSize(newWidth, newHeight));
                    log.info("📐 Resolución ajustada: {}x{}", newWidth, newHeight);
                }
            }

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp4");
            attrs.setAudioAttributes(audio);
            attrs.setVideoAttributes(video);
            // Opciones de formato para mejor compatibilidad
            attrs.setDecodingThreads(2);
            attrs.setEncodingThreads(2);

            // Crear encoder con FFmpeg correcto (ya inicializado arriba)
            Encoder encoder = new Encoder(ffmpegLocator);
            
            log.info("🎬 Iniciando compresión de video con FFmpeg...");
            log.info("⚙️ Configuración: codec={}, bitrate={}bps, formato={}",
                    video.getCodec(), video.getBitRate(), attrs.getOutputFormat());
            
            try {
                encoder.encode(multimediaObject, target, attrs);
            } catch (EncoderException e) {
                log.error("❌ Error en encoder FFmpeg: {}", e.getMessage());
                log.error("📋 Input file: {}, size: {} bytes", source.getAbsolutePath(), source.length());
                throw e;
            }

            log.info("🎥 Video comprimido: {} → {} bytes",
                    source.length(), target.length());

            return target;

        } catch (EncoderException e) {
            throw new IOException("Error comprimiendo video", e);
        }
    }

    private File extractVideoThumbnail(File videoFile, int size) throws IOException {
        try {
            Path tempThumb = Files.createTempFile("video_thumb_", ".jpg");
            File target = tempThumb.toFile();

            // Obtener FFmpeg locator para usar en MultimediaObject y Encoder
            ProcessLocator ffmpegLocator = getFFmpegLocator();
            
            MultimediaObject multimediaObject = new MultimediaObject(videoFile, ffmpegLocator);

            // Extraer frame en el segundo 1
            VideoAttributes video = new VideoAttributes();
            video.setCodec("png");
            video.setSize(new ws.schild.jave.info.VideoSize(size, size));

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("image2");
            attrs.setOffset(1.0f); // segundo 1
            attrs.setDuration(0.001f); // un solo frame
            attrs.setVideoAttributes(video);

            Encoder encoder = new Encoder(ffmpegLocator);
            encoder.encode(multimediaObject, target, attrs);

            return target;

        } catch (EncoderException e) {
            throw new IOException("Error extrayendo thumbnail de video", e);
        }
    }

    private File compressImage(File source, String extension) throws IOException {
        BufferedImage original = ImageIO.read(source);
        if (original == null) {
            throw new IOException("No se pudo leer la imagen");
        }

        BufferedImage compressed = original;

        // Redimensionar si es muy grande
        if (original.getWidth() > MAX_IMAGE_WIDTH || original.getHeight() > MAX_IMAGE_HEIGHT) {
            compressed = Scalr.resize(original,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.FIT_TO_WIDTH,
                    MAX_IMAGE_WIDTH,
                    MAX_IMAGE_HEIGHT,
                    Scalr.OP_ANTIALIAS);
        }

        Path tempOutput = Files.createTempFile("compressed_image_", "." + extension);
        File target = tempOutput.toFile();

        ImageIO.write(compressed, extension.equals("png") ? "png" : "jpg", target);

        log.info("🖼️ Imagen comprimida: {} → {} bytes",
                source.length(), target.length());

        return target;
    }

    private File createImageThumbnail(File source, int size) throws IOException {
        BufferedImage original = ImageIO.read(source);
        if (original == null) {
            throw new IOException("No se pudo leer la imagen para thumbnail");
        }

        BufferedImage thumbnail = Scalr.resize(original,
                Scalr.Method.SPEED,
                Scalr.Mode.FIT_TO_WIDTH,
                size,
                size,
                Scalr.OP_ANTIALIAS);

        Path tempOutput = Files.createTempFile("thumb_", ".jpg");
        File target = tempOutput.toFile();

        ImageIO.write(thumbnail, "jpg", target);

        return target;
    }

    private MediaType detectMediaType(String extension) {
        return switch (extension) {
            case "mp4", "mov", "avi", "mkv", "webm" -> MediaType.VIDEO;
            case "jpg", "jpeg", "png", "gif", "webp", "heic", "heif" -> MediaType.IMAGE;
            default -> MediaType.UNKNOWN;
        };
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String stripExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    public record ProcessedMedia(String mainUrl, List<String> thumbnailUrls, String type) {
    }

    private enum MediaType {
        VIDEO, IMAGE, UNKNOWN
    }
}

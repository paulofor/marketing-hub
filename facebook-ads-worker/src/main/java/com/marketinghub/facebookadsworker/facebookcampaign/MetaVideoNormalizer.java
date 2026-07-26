package com.marketinghub.facebookadsworker.facebookcampaign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Normaliza vídeos de criativos para um perfil MP4 mais compatível com upload na Meta.
 */
@Component
public class MetaVideoNormalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaVideoNormalizer.class);
    private static final int META_MIN_VIDEO_WIDTH = 1080;
    private static final int META_AUDIO_SAMPLE_RATE = 48000;
    private static final int META_AUDIO_CHANNELS = 2;

    private final boolean enabled;
    private final String ffmpegPath;
    private final Duration timeout;

    /**
     * Configura o normalizador com o executável e limite de tempo do FFmpeg.
     */
    public MetaVideoNormalizer(
        @Value("${creative.video.normalization.enabled:true}") boolean enabled,
        @Value("${creative.video.normalization.ffmpeg-path:ffmpeg}") String ffmpegPath,
        @Value("${creative.video.normalization.timeout:PT8M}") Duration timeout
    ) {
        this.enabled = enabled;
        this.ffmpegPath = StringUtils.hasText(ffmpegPath) ? ffmpegPath.trim() : "ffmpeg";
        this.timeout = timeout == null ? Duration.ofMinutes(2) : timeout;
    }

    /**
     * Reexporta o vídeo para MP4 H.264/AAC com pixel format e container previsíveis para a Meta.
     */
    public NormalizedVideo normalize(byte[] sourceBytes, String sourceFileName) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new IllegalArgumentException("sourceBytes must not be empty");
        }
        if (!enabled) {
            return new NormalizedVideo(sourceBytes, resolveOutputFileName(sourceFileName), "video/mp4", false);
        }

        Path input = null;
        Path output = null;
        Path log = null;
        try {
            input = Files.createTempFile("marketinghub-meta-video-in-", ".mp4");
            output = Files.createTempFile("marketinghub-meta-video-out-", ".mp4");
            log = Files.createTempFile("marketinghub-meta-video-", ".log");
            Files.write(input, sourceBytes);

            Process process = new ProcessBuilder(List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                input.toString(),
                "-map",
                "0:v:0",
                "-map",
                "0:a?",
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-profile:v",
                "high",
                "-level",
                "4.1",
                "-pix_fmt",
                "yuv420p",
                "-vf",
                "scale=if(lt(iw\\," + META_MIN_VIDEO_WIDTH + ")\\," + META_MIN_VIDEO_WIDTH + "\\,trunc(iw/2)*2):if(lt(iw\\," + META_MIN_VIDEO_WIDTH + ")\\,-2\\,trunc(ih/2)*2)",
                "-r",
                "30",
                "-c:a",
                "aac",
                "-ar",
                String.valueOf(META_AUDIO_SAMPLE_RATE),
                "-ac",
                String.valueOf(META_AUDIO_CHANNELS),
                "-b:a",
                "128k",
                "-movflags",
                "+faststart",
                output.toString()
            ))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg video normalization timed out after " + timeout);
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("FFmpeg video normalization failed: " + readLog(log));
            }
            byte[] normalizedBytes = Files.readAllBytes(output);
            if (normalizedBytes.length == 0) {
                throw new IllegalStateException("FFmpeg video normalization returned an empty file");
            }
            LOGGER.info(
                "Creative video normalized for Meta upload: sourceFileName={}, originalBytes={}, normalizedBytes={}",
                sourceFileName,
                sourceBytes.length,
                normalizedBytes.length
            );
            return new NormalizedVideo(normalizedBytes, resolveOutputFileName(sourceFileName), "video/mp4", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Creative video normalization interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Creative video normalization failed: " + ex.getMessage(), ex);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
            deleteQuietly(log);
        }
    }

    /**
     * Extrai um frame JPEG do vídeo normalizado para fallback de publicação quando a Meta rejeita upload de vídeo.
     */
    public NormalizedImage extractFallbackFrame(byte[] sourceBytes, String sourceFileName) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new IllegalArgumentException("sourceBytes must not be empty");
        }

        Path input = null;
        Path output = null;
        Path log = null;
        try {
            input = Files.createTempFile("marketinghub-meta-video-frame-in-", ".mp4");
            output = Files.createTempFile("marketinghub-meta-video-frame-out-", ".jpg");
            log = Files.createTempFile("marketinghub-meta-video-frame-", ".log");
            Files.write(input, sourceBytes);

            Process process = new ProcessBuilder(List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-ss",
                "00:00:01",
                "-i",
                input.toString(),
                "-frames:v",
                "1",
                "-q:v",
                "2",
                output.toString()
            ))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg video frame extraction timed out after " + timeout);
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("FFmpeg video frame extraction failed: " + readLog(log));
            }
            byte[] imageBytes = Files.readAllBytes(output);
            if (imageBytes.length == 0) {
                throw new IllegalStateException("FFmpeg video frame extraction returned an empty file");
            }
            LOGGER.info(
                "Creative video fallback frame extracted for Meta image upload: sourceFileName={}, imageBytes={}",
                sourceFileName,
                imageBytes.length
            );
            return new NormalizedImage(imageBytes, resolveFrameFileName(sourceFileName), "image/jpeg");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Creative video frame extraction interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Creative video frame extraction failed: " + ex.getMessage(), ex);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
            deleteQuietly(log);
        }
    }

    /**
     * Resolve um nome de saída sempre terminado em .mp4 para o upload normalizado.
     */
    private String resolveOutputFileName(String sourceFileName) {
        String baseName = StringUtils.hasText(sourceFileName) ? sourceFileName.trim() : "creative-" + UUID.randomUUID();
        int queryIndex = baseName.indexOf('?');
        if (queryIndex >= 0) {
            baseName = baseName.substring(0, queryIndex);
        }
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }
        if (!StringUtils.hasText(baseName)) {
            baseName = "creative-" + UUID.randomUUID();
        }
        return baseName + "-meta.mp4";
    }

    /**
     * Resolve o nome do JPEG derivado do vídeo para upload de fallback em imagem.
     */
    private String resolveFrameFileName(String sourceFileName) {
        String baseName = resolveOutputFileName(sourceFileName);
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }
        return baseName + "-fallback.jpg";
    }

    /**
     * Lê a saída do FFmpeg para diagnóstico sem expor binário de vídeo.
     */
    private String readLog(Path log) {
        if (log == null) {
            return "";
        }
        try {
            String text = Files.readString(log).trim();
            return text.length() > 1000 ? text.substring(0, 1000) : text;
        } catch (IOException ex) {
            return "could not read ffmpeg log: " + ex.getMessage();
        }
    }

    /**
     * Remove arquivos temporários sem interromper o fluxo principal.
     */
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            LOGGER.debug("Could not delete temporary video normalization file {}: {}", path, ex.getMessage());
        }
    }

    /**
     * Representa o vídeo pronto para upload na Meta após normalização.
     */
    public record NormalizedVideo(byte[] bytes, String fileName, String contentType, boolean normalized) {}

    /**
     * Representa o frame estático pronto para fallback de upload como imagem na Meta.
     */
    public record NormalizedImage(byte[] bytes, String fileName, String contentType) {}
}

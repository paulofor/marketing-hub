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
    private static final int META_VIDEO_FRAME_RATE = 30;

    private final boolean enabled;
    private final String ffmpegPath;
    private final Duration timeout;

    /**
     * Configura o normalizador com o executável e limite de tempo do FFmpeg.
     */
    public MetaVideoNormalizer(
        @Value("${creative.video.normalization.enabled:true}") boolean enabled,
        @Value("${creative.video.normalization.ffmpeg-path:ffmpeg}") String ffmpegPath,
        @Value("${creative.video.normalization.timeout:PT20M}") Duration timeout
    ) {
        this.enabled = enabled;
        this.ffmpegPath = StringUtils.hasText(ffmpegPath) ? ffmpegPath.trim() : "ffmpeg";
        this.timeout = timeout == null ? Duration.ofMinutes(20) : timeout;
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

            VideoProbe probe = probeVideo(input);
            if (probe.isMetaCompatible()) {
                LOGGER.info(
                    "Creative video already matches Meta upload profile: sourceFileName={}, width={}, height={}, videoCodec={}, pixelFormat={}, audioCodec={}, audioRate={}, audioChannels={}",
                    sourceFileName,
                    probe.width(),
                    probe.height(),
                    probe.videoCodec(),
                    probe.pixelFormat(),
                    probe.audioCodec(),
                    probe.audioSampleRate(),
                    probe.audioChannels()
                );
                return new NormalizedVideo(sourceBytes, resolveOutputFileName(sourceFileName), "video/mp4", false);
            }

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
                "ultrafast",
                "-crf",
                "28",
                "-profile:v",
                "high",
                "-level",
                "4.1",
                "-pix_fmt",
                "yuv420p",
                "-vf",
                "scale=if(lt(iw\\," + META_MIN_VIDEO_WIDTH + ")\\," + META_MIN_VIDEO_WIDTH + "\\,trunc(iw/2)*2):if(lt(iw\\," + META_MIN_VIDEO_WIDTH + ")\\,-2\\,trunc(ih/2)*2)",
                "-r",
                String.valueOf(META_VIDEO_FRAME_RATE),
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
     * Inspeciona o arquivo com ffprobe para decidir se a transcodificação pesada é realmente necessária.
     */
    private VideoProbe probeVideo(Path input) {
        Path log = null;
        try {
            log = Files.createTempFile("marketinghub-meta-video-probe-", ".log");
            Process process = new ProcessBuilder(List.of(
                resolveFfprobePath(),
                "-v",
                "error",
                "-select_streams",
                "v:0",
                "-show_entries",
                "stream=codec_name,width,height,pix_fmt",
                "-of",
                "csv=p=0",
                input.toString()
            ))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();

            boolean finished = process.waitFor(Math.min(timeout.toMillis(), Duration.ofSeconds(20).toMillis()), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warn("FFprobe video inspection timed out; falling back to normalization");
                return VideoProbe.requiresNormalization();
            }
            if (process.exitValue() != 0) {
                LOGGER.warn("FFprobe video inspection failed; falling back to normalization: {}", readLog(log));
                return VideoProbe.requiresNormalization();
            }
            String[] values = Files.readString(log).trim().split(",");
            if (values.length < 4) {
                return VideoProbe.requiresNormalization();
            }
            AudioProbe audio = probeAudio(input);
            return new VideoProbe(
                parseInt(values[1]),
                parseInt(values[2]),
                values[0],
                values[3],
                audio.codec(),
                audio.sampleRate(),
                audio.channels()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Creative video inspection interrupted; falling back to normalization", ex);
            return VideoProbe.requiresNormalization();
        } catch (IOException ex) {
            LOGGER.warn("Creative video inspection failed; falling back to normalization: {}", ex.getMessage(), ex);
            return VideoProbe.requiresNormalization();
        } finally {
            deleteQuietly(log);
        }
    }

    /**
     * Inspeciona a trilha de áudio para reaproveitar vídeos que já têm AAC 48 kHz estéreo.
     */
    private AudioProbe probeAudio(Path input) {
        Path log = null;
        try {
            log = Files.createTempFile("marketinghub-meta-video-audio-probe-", ".log");
            Process process = new ProcessBuilder(List.of(
                resolveFfprobePath(),
                "-v",
                "error",
                "-select_streams",
                "a:0",
                "-show_entries",
                "stream=codec_name,sample_rate,channels",
                "-of",
                "csv=p=0",
                input.toString()
            ))
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
            boolean finished = process.waitFor(Math.min(timeout.toMillis(), Duration.ofSeconds(20).toMillis()), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return AudioProbe.requiresNormalization();
            }
            if (process.exitValue() != 0) {
                return AudioProbe.noAudio();
            }
            String text = Files.readString(log).trim();
            if (!StringUtils.hasText(text)) {
                return AudioProbe.noAudio();
            }
            String[] values = text.split(",");
            if (values.length < 3) {
                return AudioProbe.requiresNormalization();
            }
            return new AudioProbe(values[0], parseInt(values[1]), parseInt(values[2]));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return AudioProbe.requiresNormalization();
        } catch (IOException ex) {
            return AudioProbe.requiresNormalization();
        } finally {
            deleteQuietly(log);
        }
    }

    /**
     * Resolve o binário ffprobe coerente com o caminho configurado para ffmpeg.
     */
    private String resolveFfprobePath() {
        if (ffmpegPath.endsWith("ffmpeg")) {
            return ffmpegPath.substring(0, ffmpegPath.length() - "ffmpeg".length()) + "ffprobe";
        }
        return "ffprobe";
    }

    /**
     * Converte valores numéricos do ffprobe em inteiro seguro.
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ex) {
            return 0;
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

    /**
     * Representa os metadados técnicos usados para decidir se a Meta já aceitará o vídeo.
     */
    private record VideoProbe(
        int width,
        int height,
        String videoCodec,
        String pixelFormat,
        String audioCodec,
        int audioSampleRate,
        int audioChannels
    ) {
        /**
         * Cria uma inspeção que obriga normalização por falta de metadados confiáveis.
         */
        private static VideoProbe requiresNormalization() {
            return new VideoProbe(0, 0, "", "", "", 0, 0);
        }

        /**
         * Indica se o arquivo já atende ao perfil técnico usado para criativos Meta.
         */
        private boolean isMetaCompatible() {
            boolean videoMatches = width >= META_MIN_VIDEO_WIDTH
                && height > 0
                && width % 2 == 0
                && height % 2 == 0
                && "h264".equalsIgnoreCase(videoCodec)
                && "yuv420p".equalsIgnoreCase(pixelFormat);
            boolean audioMatches = !StringUtils.hasText(audioCodec)
                || ("aac".equalsIgnoreCase(audioCodec)
                    && audioSampleRate == META_AUDIO_SAMPLE_RATE
                    && audioChannels == META_AUDIO_CHANNELS);
            return videoMatches && audioMatches;
        }
    }

    /**
     * Representa os metadados da trilha de áudio do vídeo.
     */
    private record AudioProbe(String codec, int sampleRate, int channels) {
        /**
         * Representa vídeo sem áudio, permitido no fluxo de criativo.
         */
        private static AudioProbe noAudio() {
            return new AudioProbe("", 0, 0);
        }

        /**
         * Cria uma inspeção que obriga normalização por falta de metadados confiáveis.
         */
        private static AudioProbe requiresNormalization() {
            return new AudioProbe("unknown", 0, 0);
        }
    }
}

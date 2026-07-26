package com.marketinghub.facebookadsworker.facebookcampaign;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Valida a normalização técnica dos vídeos de criativos antes do upload para a Meta.
 */
class MetaVideoNormalizerTest {

    /**
     * Garante que vídeos verticais 720p sejam convertidos para largura mínima aceita pela Meta e áudio 48 kHz.
     */
    @Test
    void normalizesSmallVerticalVideoToMetaMinimumWidthAndAudioRate() throws Exception {
        assumeTrue(commandExists("ffmpeg"), "ffmpeg indisponível no ambiente de teste");
        assumeTrue(commandExists("ffprobe"), "ffprobe indisponível no ambiente de teste");

        Path source = Files.createTempFile("meta-video-source-", ".mp4");
        Path normalized = Files.createTempFile("meta-video-normalized-", ".mp4");
        try {
            run(List.of(
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "color=c=black:s=720x1280:d=1:r=25",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=1000:duration=1:sample_rate=48000",
                "-c:v",
                "libx264",
                "-pix_fmt",
                "yuv420p",
                "-c:a",
                "aac",
                "-shortest",
                source.toString()
            ));

            MetaVideoNormalizer normalizer = new MetaVideoNormalizer(true, "ffmpeg", Duration.ofSeconds(60));
            MetaVideoNormalizer.NormalizedVideo result = normalizer.normalize(Files.readAllBytes(source), "creative.mp4");
            Files.write(normalized, result.bytes());

            String probe = run(List.of(
                "ffprobe",
                "-v",
                "error",
                "-show_entries",
                "stream=codec_type,width,height,sample_rate,channels",
                "-of",
                "json",
                normalized.toString()
            ));

            assertTrue(probe.contains("\"width\": 1080"), probe);
            assertTrue(probe.contains("\"height\": 1920"), probe);
            assertTrue(probe.contains("\"sample_rate\": \"48000\""), probe);
            assertTrue(probe.contains("\"channels\": 2"), probe);
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(normalized);
        }
    }

    /**
     * Garante que vídeos já compatíveis com a Meta não sejam reexportados sem necessidade.
     */
    @Test
    void keepsAlreadyCompatibleMetaVideoWithoutReencoding() throws Exception {
        assumeTrue(commandExists("ffmpeg"), "ffmpeg indisponível no ambiente de teste");
        assumeTrue(commandExists("ffprobe"), "ffprobe indisponível no ambiente de teste");

        Path source = Files.createTempFile("meta-video-ready-", ".mp4");
        try {
            run(List.of(
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "color=c=black:s=1080x1920:d=1:r=30",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=1000:duration=1:sample_rate=48000",
                "-c:v",
                "libx264",
                "-pix_fmt",
                "yuv420p",
                "-c:a",
                "aac",
                "-ac",
                "2",
                "-shortest",
                source.toString()
            ));

            byte[] original = Files.readAllBytes(source);
            MetaVideoNormalizer normalizer = new MetaVideoNormalizer(true, "ffmpeg", Duration.ofSeconds(60));
            MetaVideoNormalizer.NormalizedVideo result = normalizer.normalize(original, "creative.mp4");

            assertFalse(result.normalized());
            assertArrayEquals(original, result.bytes());
        } finally {
            Files.deleteIfExists(source);
        }
    }

    /**
     * Verifica se o comando está disponível sem falhar o teste em ambientes mínimos.
     */
    private boolean commandExists(String command) throws Exception {
        Process process;
        try {
            process = new ProcessBuilder(command, "-version")
                .redirectErrorStream(true)
                .start();
        } catch (IOException ex) {
            return false;
        }
        return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
    }

    /**
     * Executa comando externo e devolve a saída textual para asserções técnicas.
     */
    private String run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes());
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Comando excedeu o tempo limite: " + command);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Comando falhou: " + command + "\n" + output);
        }
        return output;
    }
}

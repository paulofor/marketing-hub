package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Homologa ffprobe, cenas, áudio e contact sheets com uma mídia local controlada. */
class ReferenceMediaInspectorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private Path video;

    /** Gera vídeo sintético e inicia origem HTTP descartável. */
    @BeforeEach
    void setUp() throws Exception {
        video = Files.createTempFile("reference-inspector-", ".mp4");
        Process process = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "testsrc2=duration=2:size=320x568:rate=30",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=2", "-shortest",
                "-c:v", "mpeg4", "-c:a", "aac", video.toString()).start();
        assertThat(process.waitFor()).isZero();
        byte[] payload = Files.readAllBytes(video);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reference.mp4", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "video/mp4");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
    }

    /** Encerra origem e remove mídia após cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        if (server != null) server.stop(0);
        Files.deleteIfExists(video);
    }

    /** Extrai evidência técnica e dois painéis sem persistir os binários temporários. */
    @Test
    void shouldInspectVideoAndBuildTwoContactSheets() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getReferenceAnalysis().setAllowPrivateSourceUrls(true);
        ReferenceMediaInspector inspector = new ReferenceMediaInspector(properties, objectMapper);
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.mp4";
        ReferenceAnalysisStageContext context = new ReferenceAnalysisStageContext(
                21L, 4L, "test", 1, "producer-21",
                objectMapper.readTree("{\"sourceUrl\":\"" + url + "\"}"), Instant.now());

        var evidence = inspector.inspect(context);

        assertThat(evidence.artifacts().path("durationSeconds").asDouble()).isBetween(1.9, 2.1);
        assertThat(evidence.artifacts().path("width").asInt()).isEqualTo(320);
        assertThat(evidence.artifacts().path("height").asInt()).isEqualTo(568);
        assertThat(evidence.artifacts().path("sha256").asText()).hasSize(64);
        assertThat(evidence.contactSheetDataUrls()).hasSize(2).allMatch(value -> value.startsWith("data:image/jpeg;base64,"));
    }

    /** Bloqueia origens locais por padrão para impedir acesso indevido à rede do executor. */
    @Test
    void shouldRejectPrivateSourceByDefault() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        ReferenceMediaInspector inspector = new ReferenceMediaInspector(properties, objectMapper);
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.mp4";
        ReferenceAnalysisStageContext context = new ReferenceAnalysisStageContext(
                22L, 4L, "test", 1, "producer-22",
                objectMapper.readTree("{\"sourceUrl\":\"" + url + "\"}"), Instant.now());

        assertThatThrownBy(() -> inspector.inspect(context))
                .hasMessageContaining("rede privada ou local");
    }
}

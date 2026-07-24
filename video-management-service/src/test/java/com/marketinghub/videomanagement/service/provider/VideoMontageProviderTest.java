package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a montagem local de múltiplos clipes de venda. */
class VideoMontageProviderTest {
    private MockWebServer server;

    /** Inicializa o servidor HTTP usado para entregar clipes fonte. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o servidor HTTP após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve baixar dois clipes e devolver MP4 único com metadata de montagem. */
    @Test
    void shouldCreateMontageFromMultipleSourceVideos() throws Exception {
        server.enqueue(mp4Response());
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        VideoMontageProvider provider =
                new VideoMontageProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("montage-77");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-77-musa-montage.mp4");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "MUSA_VIDEO_MONTAGE")
                .containsEntry("source_count", 2)
                .containsEntry("resolution", "720x1280")
                .containsKey("audio");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/scene-1.mp4");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/scene-2.mp4");
    }

    /** Cria uma resposta MP4 mínima para o download fonte. */
    private MockResponse mp4Response() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody(new Buffer().write(new byte[] {
                        0, 0, 0, 32, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0
                }));
    }

    /** Configura o provider com ffmpeg fake para manter o teste determinístico. */
    private VideoManagementProperties properties() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getPostProduction().setEnabled(true);
        properties.getProviders().getPostProduction().setFfmpegPath(fakeFfmpeg().toString());
        return properties;
    }

    /** Cria um script executável que simula normalização e concatenação por ffmpeg. */
    private Path fakeFfmpeg() throws Exception {
        Path script = Files.createTempFile("fake-ffmpeg-montage", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                output=""
                for arg in "$@"; do
                  output="$arg"
                done
                printf '\\000\\000\\000\\040ftypisom\\000\\000\\002\\000' > "$output"
                exit 0
                """);
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um job de montagem com dois clipes fonte. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                77L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "MUSA_VIDEO_MONTAGE",
                null,
                SalesVideoJobType.POST_PRODUCTION,
                SalesVideoStatus.VIDEO_REQUESTED,
                1,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                """
                        {
                          "sourceJobIds": [10, 11],
                          "sourceVideos": [
                            {"sourceJobId": 10, "sourceVideoUrl": "/source/scene-1.mp4"},
                            {"sourceJobId": 11, "sourceVideoUrl": "/source/scene-2.mp4"}
                          ]
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um perfil mínimo para a execução do provider. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "script text",
                "hook",
                "cta",
                "caption",
                null,
                "MANUAL",
                "gpt",
                "prompt",
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                1L,
                null,
                "HERO",
                "Título",
                "Persona",
                "Estilo",
                "Voz",
                "pt-BR",
                30,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}

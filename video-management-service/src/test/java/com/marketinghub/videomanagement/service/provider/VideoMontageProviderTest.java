package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private Path ffmpegArguments;
    private boolean sourceHasAudio = true;

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
        assertThat(artifacts.metadata().get("audio").toString())
                .contains(
                        "preserved=true",
                        "normalized=true",
                        "source_audio_count=2",
                        "silent_scene_count=0",
                        "transition=ACROSSFADE");
        assertThat(Files.readString(ffmpegArguments))
                .contains("acrossfade=d=0.18", "-c:a", "aac")
                .doesNotContain("-an");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/scene-1.mp4");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/scene-2.mp4");
    }

    /** Normaliza cenas mudas com silêncio para manter a montagem previsível. */
    @Test
    void shouldNormalizeSilentSourcesWithoutInventingMusic() throws Exception {
        sourceHasAudio = false;
        server.enqueue(mp4Response());
        server.enqueue(mp4Response());
        VideoMontageProvider provider =
                new VideoMontageProvider(properties(), new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata().get("audio").toString())
                .contains(
                        "preserved=false",
                        "source_audio_count=0",
                        "silent_scene_count=2",
                        "transition=ACROSSFADE");
        assertThat(Files.readString(ffmpegArguments))
                .contains("anullsrc=r=48000:cl=stereo", "acrossfade=d=0.18");
    }

    /** Bloqueia áudio de URL avulsa quando a cena não possui linhagem de provider. */
    @Test
    void shouldRejectSourcesWithoutAuditableProviderLineage() throws Exception {
        server.enqueue(mp4Response());
        VideoMontageProvider provider =
                new VideoMontageProvider(properties(), new ObjectMapper(), WebClient.builder());
        SalesVideoJob job = jobWithMetadata("""
                {
                  "sourceJobIds": [10, 11],
                  "sourceVideos": [
                    {"sourceJobId": 10, "sourceVideoUrl": "/source/scene-1.mp4"},
                    {"sourceJobId": 11, "sourceVideoUrl": "/source/scene-2.mp4"}
                  ]
                }
                """);

        assertThatThrownBy(() -> provider.render(job, profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("linhagem auditável");
        assertThat(server.getRequestCount()).isZero();
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
        properties.getProviders().getPostProduction().setFfprobePath(fakeFfprobe().toString());
        return properties;
    }

    /** Cria um script executável que simula normalização e concatenação por ffmpeg. */
    private Path fakeFfmpeg() throws Exception {
        ffmpegArguments = Files.createTempFile("fake-ffmpeg-montage-arguments", ".txt");
        Path script = Files.createTempFile("fake-ffmpeg-montage", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                printf '%%s\\n' "$@" >> '%s'
                output=""
                for arg in "$@"; do
                  output="$arg"
                done
                printf '\\000\\000\\000\\040ftypisom\\000\\000\\002\\000' > "$output"
                exit 0
                """.formatted(ffmpegArguments));
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um script executável que simula a auditoria de duração do ffprobe. */
    private Path fakeFfprobe() throws Exception {
        Path script = Files.createTempFile("fake-ffprobe-montage", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                audio_probe="false"
                for argument in "$@"; do
                  if [ "$argument" = "stream=index" ]; then audio_probe="true"; fi
                done
                if [ "$audio_probe" = "true" ]; then
                  %s
                  exit 0
                fi
                printf '20.000000\\n'
                exit 0
                """.formatted(sourceHasAudio ? "printf '0\\n'" : ":"));
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um job de montagem com dois clipes fonte. */
    private SalesVideoJob job() {
        return jobWithMetadata("""
                {
                  "sourceJobIds": [10, 11],
                  "sourceVideos": [
                    {"sourceJobId": 10, "sourceVideoUrl": "/source/scene-1.mp4", "sourceProviderName": "RUNWAY_GEN_4_5"},
                    {"sourceJobId": 11, "sourceVideoUrl": "/source/scene-2.mp4", "sourceProviderName": "RUNWAY_GEN_4_5"}
                  ]
                }
                """);
    }

    /** Cria um job mínimo com metadata variável para validar contratos de origem. */
    private SalesVideoJob jobWithMetadata(String metadataJson) {
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
                metadataJson,
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

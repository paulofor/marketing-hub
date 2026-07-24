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

/** Responsabilidade: validar a pós-produção local de vídeos de venda. */
class PostProductionVideoProviderTest {
    private MockWebServer server;

    /** Inicializa o servidor HTTP usado para entregar o MP4 fonte. */
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

    /** Deve baixar o vídeo bruto e devolver MP4 final com legenda VTT e metadados comerciais. */
    @Test
    void shouldPostProduceVideoWithVoiceCaptionsAndMusicMetadata() throws Exception {
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("post-production-55");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-55-musa-final.mp4");
        assertThat(artifacts.captionFile().assetType()).isEqualTo(AssetType.CAPTION);
        assertThat(new String(artifacts.captionFile().content())).contains("WEBVTT", "Pare de se sentir comum");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "MUSA_POST_PRODUCTION")
                .containsEntry("duration_seconds", 30)
                .containsKey("audio")
                .containsKey("captions");
        assertThat(artifacts.metadata().get("audio").toString())
                .contains("BLOCKED_FOR_CAMPAIGN", "synthetic_local");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/musa.mp4");
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

    /** Configura o provider com binários fake para manter o teste determinístico. */
    private VideoManagementProperties properties() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getPostProduction().setEnabled(true);
        properties.getProviders().getPostProduction().setEspeakPath(fakeEspeak().toString());
        properties.getProviders().getPostProduction().setFfmpegPath(fakeFfmpeg().toString());
        properties.getProviders().getPostProduction().setFontFile("/tmp/fake-font.ttf");
        return properties;
    }

    /** Cria um script executável que simula voz off em WAV. */
    private Path fakeEspeak() throws Exception {
        Path script = Files.createTempFile("fake-espeak", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                output=""
                previous=""
                for arg in "$@"; do
                  if [ "$previous" = "-w" ]; then output="$arg"; fi
                  previous="$arg"
                done
                printf 'RIFF....WAVEfmt ' > "$output"
                exit 0
                """);
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um script executável que simula ffmpeg escrevendo MP4 final. */
    private Path fakeFfmpeg() throws Exception {
        Path script = Files.createTempFile("fake-ffmpeg-post", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                output=""
                for arg in "$@"; do
                  output="$arg"
                done
                if [ "$output" = "-" ]; then
                  echo '    I:         -26.3 LUFS'
                  echo '    Peak:       -7.6 dBFS'
                  exit 0
                fi
                printf '\\000\\000\\000\\040ftypisom\\000\\000\\002\\000' > "$output"
                exit 0
                """);
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um job de pós-produção com vídeo fonte e textos comerciais. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                55L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "MUSA_POST_PRODUCTION",
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
                          "artifactType": "experiment.videoPostProductionRequest.v1",
                          "experimentVideoAssetId": 5,
                          "sourceVideoUrl": "/source/musa.mp4",
                          "voiceOverScript": "Você se arruma e sente que falta presença. Veja seu plano MUSA.",
                          "captionText": "Pare de se sentir comum no espelho. Veja seu plano MUSA de 7 dias."
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

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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o adapter direto da Runway no módulo de vídeo. */
class RunwayVideoProviderTest {
    private MockWebServer server;

    /** Inicializa a API Runway simulada para capturar criação, polling e download. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API Runway simulada após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve criar uma tarefa Runway, aguardar sucesso e devolver MP4 auditável. */
    @Test
    void shouldRenderRunwayJobThroughImageToVideoApi() throws Exception {
        server.enqueue(json("""
                {"id":"runway-task-123"}
                """));
        server.enqueue(json("""
                {
                  "id": "runway-task-123",
                  "status": "SUCCEEDED",
                  "output": [
                    "%s/download/runway-task-123.mp4"
                  ]
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        AtomicInteger progress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job(), profile(),
                (percent, status, message) -> progress.set(percent == null ? 0 : percent));

        assertThat(provider.supports(job())).isTrue();
        assertThat(provider.supports(job("RUNAWAY"))).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("runway-task-123");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-1-runway.mp4");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "RUNWAY")
                .containsEntry("model", "gen4.5")
                .containsEntry("duration_seconds", 10);
        assertThat(progress.get()).isEqualTo(95);
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/image_to_video");
        assertThat(createRequest.getHeader("Authorization")).isEqualTo("Bearer runway-test-key");
        assertThat(createRequest.getHeader("X-Runway-Version")).isEqualTo("2024-11-06");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"model\":\"gen4.5\"")
                .contains("\"ratio\":\"720:1280\"")
                .contains("\"duration\":10")
                .contains("\"promptImage\":\"https://assets.example/musa-character.png\"")
                .contains("Very sharp image, crisp focus and constant soft natural daylight")
                .contains("Avoid embedded text");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/tasks/runway-task-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/runway-task-123.mp4");
    }

    /** Deve falhar cedo quando a chave Runway não estiver configurada. */
    @Test
    void shouldRejectMissingRunwayApiKey() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getRunway().setApiKey("");
        RunwayVideoProvider provider = new RunwayVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("RUNWAY_API_KEY");
    }

    /** Deve aceitar chave Runway montada em arquivo de secret no container. */
    @Test
    void shouldReadRunwayApiKeyFromSecretFile() throws Exception {
        server.enqueue(json("""
                {"id":"runway-task-123"}
                """));
        server.enqueue(json("""
                {
                  "id": "runway-task-123",
                  "status": "SUCCEEDED",
                  "output": [
                    "%s/download/runway-task-123.mp4"
                  ]
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        Path keyFile = Files.createTempFile("runway-api-key", ".txt");
        Files.writeString(keyFile, "runway-file-key\n");
        VideoManagementProperties properties = properties();
        properties.getProviders().getRunway().setApiKey(null);
        properties.getProviders().getRunway().setApiKeyFile(keyFile.toString());
        RunwayVideoProvider provider = new RunwayVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer runway-file-key");
        Files.deleteIfExists(keyFile);
    }

    /** Cria uma resposta JSON para a API Runway simulada. */
    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    /** Cria uma resposta MP4 mínima com assinatura ftyp. */
    private MockResponse mp4Response() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody(new Buffer().write(new byte[] {
                        0, 0, 0, 32, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0
                }));
    }

    /** Configura o provider Runway apontando para a API simulada. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getRunway().setEnabled(true);
        properties.getProviders().getRunway().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getRunway().setApiKey("runway-test-key");
        properties.getProviders().getRunway().setPollInterval(Duration.ofMillis(1));
        return properties;
    }

    /** Cria um job de render Runway com imagem de personagem como primeiro frame. */
    private SalesVideoJob job() {
        return job("RUNWAY");
    }

    /** Cria um job de render para o provider informado. */
    private SalesVideoJob job(String providerName) {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                providerName,
                null,
                SalesVideoJobType.RENDER,
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
                          "characterImageReferenceUrl": "https://assets.example/musa-character.png",
                          "assembly_plan": {
                            "scenes": [
                              {"role":"DOR","title":"Dor cotidiana","message":"Ela se arruma, mas sente que falta presença."},
                              {"role":"MECANISMO","title":"Microações","message":"Reduzir ruído visual, cor e acabamento."},
                              {"role":"CTA","title":"Diagnóstico gratuito","message":"Veja seu plano de 7 dias."}
                            ]
                          },
                          "visual_provider_directives": "Very sharp image, crisp focus and constant soft natural daylight."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um perfil com roteiro aprovado para o Runway. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "Você se arruma, mas sente que falta presença. O método mostra uma microação prática.",
                "Você se arruma e ainda sente que falta algo?",
                "Ver meu plano",
                "Faça o diagnóstico gratuito.",
                null,
                "MANUAL",
                null,
                null,
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                4L,
                null,
                "AD",
                "Teste Runway",
                "Consumidora brasileira adulta",
                "natural e prático",
                "voz direta",
                "pt-BR",
                10,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}

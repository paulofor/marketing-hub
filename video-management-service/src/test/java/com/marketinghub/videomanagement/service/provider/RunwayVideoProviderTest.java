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
import java.math.BigDecimal;
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
                .contains("REQUIRED SCENE ACTION: remover dois acessorios e escolher a peca-sinal")
                .contains("Very sharp image, crisp focus and constant soft natural daylight")
                .contains("Do not render letters, words, captions")
                .contains("added only in post-production");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/tasks/runway-task-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/runway-task-123.mp4");
    }

    /** Deve usar text-to-video e respeitar o limite oficial quando a cena não possui imagem-base. */
    @Test
    void shouldRenderTextToVideoWithPromptLimitedToOneThousandUtf16Units() throws Exception {
        server.enqueue(json("""
                {"id":"runway-text-task"}
                """));
        server.enqueue(json("""
                {
                  "id": "runway-text-task",
                  "status": "SUCCEEDED",
                  "output": ["%s/download/runway-text-task.mp4"]
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        provider.render(jobWithoutReferenceImage(), profile(), (percent, status, message) -> { });

        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/text_to_video");
        String requestBody = createRequest.getBody().readUtf8();
        String prompt = new ObjectMapper().readTree(requestBody).path("promptText").asText();
        assertThat(prompt.length()).isLessThanOrEqualTo(1000);
        assertThat(requestBody).doesNotContain("promptImage");
    }

    /** Deve rotear Seedance 2 pelo mesmo adapter e pela mesma chave da Runway. */
    @Test
    void shouldRenderSeedance2WithRunwayCredentials() throws Exception {
        server.enqueue(json("""
                {"id":"seedance-task"}
                """));
        server.enqueue(json("""
                {"id":"seedance-task","status":"SUCCEEDED","output":["%s/download/seedance.mp4"]}
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        provider.render(job("RUNWAY_SEEDANCE_2"), profile(), (percent, status, message) -> { });

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer runway-test-key");
        assertThat(request.getBody().readUtf8()).contains("\"model\":\"seedance2\"");
    }

    /** Deve preservar o identificador do Seedance 2.5 usando a mesma credencial da Runway. */
    @Test
    void shouldRenderSeedance25WithRunwayCredentialsAndOwnModelId() throws Exception {
        server.enqueue(json("""
                {"id":"seedance-25-task"}
                """));
        server.enqueue(json("""
                {"id":"seedance-25-task","status":"SUCCEEDED","output":["%s/download/seedance-25.mp4"]}
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        provider.render(job("RUNWAY_SEEDANCE_2_5"), profile(), (percent, status, message) -> { });

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer runway-test-key");
        assertThat(request.getBody().readUtf8())
                .contains("\"model\":\"seedance2_5\"")
                .doesNotContain("\"model\":\"seedance2\"");
    }

    /** Deve rotear os modelos comerciais curados pelo mesmo token da Runway. */
    @Test
    void shouldRouteCuratedRunwayModelsWithCompatibleDurations() throws Exception {
        for (String providerName : java.util.List.of(
                "RUNWAY_GEN_4_TURBO", "RUNWAY_HAILUO_3", "RUNWAY_GROK_IMAGINE_1_5",
                "RUNWAY_VEO_3_1_FAST", "RUNWAY_VEO_3_1")) {
            server.enqueue(json("{\"id\":\"curated-task\"}"));
            server.enqueue(json("""
                    {"id":"curated-task","status":"SUCCEEDED","output":["%s/download/curated.mp4"]}
                    """.formatted(server.url("/").toString().replaceAll("/$", ""))));
            server.enqueue(mp4Response());
            RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

            provider.render(job(providerName), profile(), (percent, status, message) -> { });

            RecordedRequest request = server.takeRequest();
            String body = request.getBody().readUtf8();
            if (providerName.equals("RUNWAY_GEN_4_TURBO")) {
                assertThat(body).contains("\"model\":\"gen4_turbo\"").contains("\"duration\":10");
            } else if (providerName.equals("RUNWAY_HAILUO_3")) {
                assertThat(body).contains("\"model\":\"hailuo3\"").contains("\"duration\":10");
            } else if (providerName.equals("RUNWAY_GROK_IMAGINE_1_5")) {
                assertThat(body).contains("\"model\":\"grok_imagine_1_5\"").contains("\"duration\":10");
            } else if (providerName.equals("RUNWAY_VEO_3_1_FAST")) {
                assertThat(body).contains("\"model\":\"veo3.1_fast\"").contains("\"duration\":8");
            } else {
                assertThat(body).contains("\"model\":\"veo3.1\"").contains("\"duration\":8");
            }
            server.takeRequest();
            server.takeRequest();
        }
    }

    /** Deve bloquear Grok Imagine sem imagem-base antes de consumir créditos. */
    @Test
    void shouldRejectGrokImagineWithoutReferenceImage() {
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        SalesVideoJob base = jobWithoutReferenceImage();
        SalesVideoJob job = new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                "RUNWAY_GROK_IMAGINE_1_5", base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                base.metadataJson(), base.createdAt(), base.updatedAt());

        assertThatThrownBy(() -> provider.render(job, profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("imagem-base aprovada");
    }

    /** Deve bloquear Gen-4 Turbo sem imagem-base antes de consumir créditos. */
    @Test
    void shouldRejectGen4TurboWithoutReferenceImage() {
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        SalesVideoJob base = jobWithoutReferenceImage();
        SalesVideoJob job = new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                "RUNWAY_GEN_4_TURBO", base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                base.metadataJson(), base.createdAt(), base.updatedAt());

        assertThatThrownBy(() -> provider.render(job, profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("imagem-base");
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

    /** Deve separar funções comerciais e contabilizar o custo de todas as cenas planejadas. */
    @Test
    void shouldPlanDistinctCommercialScenesAndFullMontageCost() throws Exception {
        RunwayVideoProvider provider = new RunwayVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        var sceneDirective = RunwayVideoProvider.class.getDeclaredMethod("sceneDirective", int.class, int.class);
        sceneDirective.setAccessible(true);
        var estimateCost = RunwayVideoProvider.class.getDeclaredMethod(
                "estimateCostUsd", String.class, int.class, int.class);
        estimateCost.setAccessible(true);
        var resolveDuration = RunwayVideoProvider.class.getDeclaredMethod(
                "resolveDuration", SalesVideoJob.class, VideoManagementProperties.Runway.class,
                com.fasterxml.jackson.databind.JsonNode.class);
        resolveDuration.setAccessible(true);

        assertThat(sceneDirective.invoke(provider, 1, 4)).asString().contains("DOR");
        assertThat(sceneDirective.invoke(provider, 2, 4)).asString().contains("RESULTADO");
        assertThat(sceneDirective.invoke(provider, 3, 4)).asString().contains("MECANISMO");
        assertThat(sceneDirective.invoke(provider, 4, 4)).asString().contains("CTA");
        assertThat((BigDecimal) estimateCost.invoke(provider, "gen4.5", 10, 3))
                .isEqualByComparingTo("3.60");
        assertThat(resolveDuration.invoke(
                provider,
                job("RUNWAY_SEEDANCE_2_5"),
                properties().getProviders().getRunway(),
                new ObjectMapper().readTree("{\"providerClipDurationSeconds\":15}")))
                .isEqualTo(15);
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
                          "scene": {
                            "role": "MECANISMO",
                            "prompt": "remover dois acessorios e escolher a peca-sinal"
                          },
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

    /** Cria uma cena Runway sem imagem-base para validar a modalidade text-to-video. */
    private SalesVideoJob jobWithoutReferenceImage() {
        SalesVideoJob base = job();
        return new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                base.providerName(), base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                "{\"generation_strategy\":\"SCENE_BY_SCENE_MONTAGE\",\"scene\":{\"role\":\"MECANISMO\"}}",
                base.createdAt(), base.updatedAt());
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

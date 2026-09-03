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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o adapter direto do VEO/Gemini no módulo de vídeo. */
class VeoVideoProviderTest {
    private MockWebServer server;

    /** Inicializa a Gemini API simulada para capturar geração, polling e download. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a Gemini API simulada após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve renderizar um job VEO chamando predictLongRunning, operação e download do MP4. */
    @Test
    void shouldRenderVeoJobThroughGeminiApi() throws Exception {
        server.enqueue(json("{\"name\":\"operations/video-123\"}"));
        server.enqueue(json("""
                {
                  "done": true,
                  "response": {
                    "generateVideoResponse": {
                      "generatedSamples": [
                        {"video": {"uri": "%s/download/video-123"}}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(new MockResponse()
                .setResponseCode(302)
                .setHeader("Location", server.url("/download/video-123-final").toString())
                .setBody("""
                        {"error":{"code":302,"message":"Unknown Error.","status":"UNKNOWN"}}
                        """));
        server.enqueue(mp4Response());
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        AtomicInteger progress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job(), profile(),
                (percent, status, message) -> progress.set(percent == null ? 0 : percent));

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.videoFile()).isNotNull();
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.providerJobId()).isEqualTo("operations/video-123");
        assertThat(progress.get()).isEqualTo(95);
        RecordedRequest renderRequest = server.takeRequest();
        assertThat(renderRequest.getPath()).isEqualTo("/models/veo-3.1-generate-preview:predictLongRunning");
        assertThat(renderRequest.getBody().readUtf8())
                .contains("mulher brasileira elegante")
                .contains("https://assets.example/musa-character.png")
                .contains("Very sharp image, crisp focus and constant soft natural daylight");
        assertThat(server.takeRequest().getPath()).isEqualTo("/operations/video-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/video-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/video-123-final");
    }

    /** Deve rejeitar resposta JSON/erro para impedir asset falso com extensão MP4. */
    @Test
    void shouldRejectNonMp4Download() {
        server.enqueue(json("{\"name\":\"operations/video-123\"}"));
        server.enqueue(json("""
                {
                  "done": true,
                  "response": {
                    "generateVideoResponse": {
                      "generatedSamples": [
                        {"video": {"uri": "%s/download/video-123"}}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"code\":302}}"));
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("não retornou MP4 válido");
    }

    /** Deve aceitar REAL como alias legado para jobs de experimento roteados ao VEO. */
    @Test
    void shouldSupportRealAliasForLegacyExperimentJobs() {
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThat(provider.supports(job("REAL"))).isTrue();
    }

    /** Deve aceitar chave Gemini montada em arquivo de secret no container. */
    @Test
    void shouldReadGeminiApiKeyFromSecretFile() throws Exception {
        server.enqueue(json("{\"name\":\"operations/video-123\"}"));
        server.enqueue(json("""
                {
                  "done": true,
                  "response": {
                    "generateVideoResponse": {
                      "generatedSamples": [
                        {"video": {"uri": "%s/download/video-123"}}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        Path keyFile = Files.createTempFile("gemini-api-key", ".txt");
        Files.writeString(keyFile, "gemini-file-key\n");
        VideoManagementProperties properties = properties();
        properties.getProviders().getVeo().setApiKey(null);
        properties.getProviders().getVeo().setApiKeyFile(keyFile.toString());
        VeoVideoProvider provider = new VeoVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getHeader("x-goog-api-key")).isEqualTo("gemini-file-key");
        Files.deleteIfExists(keyFile);
    }

    /** Deve animar a imagem aprovada em três cenas, conciliar custo e montar o vídeo integral. */
    @Test
    void shouldRenderThreeGovernedScenesFromApprovedImage() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody(new Buffer().write(new byte[] {(byte) 0x89, 'P', 'N', 'G'})));
        byte[] clip = validMp4();
        for (int scene = 1; scene <= 3; scene++) {
            server.enqueue(json("{\"name\":\"operations/vega-scene-%d\"}".formatted(scene)));
            server.enqueue(json("""
                    {
                      "done": true,
                      "response": {
                        "generateVideoResponse": {
                          "generatedSamples": [
                            {"video": {"uri": "%s/download/vega-scene-%d"}}
                          ]
                        }
                      }
                    }
                    """.formatted(server.url("/").toString().replaceAll("/$", ""), scene)));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "video/mp4")
                    .setBody(new Buffer().write(clip)));
        }
        VideoManagementProperties properties = properties();
        properties.getProviders().getVeo().setModel("veo-3.1-fast-generate-preview");
        VeoVideoProvider provider = new VeoVideoProvider(properties, new ObjectMapper(), WebClient.builder());
        List<String> financialEvents = new ArrayList<>();
        ProgressCallback callback = new ProgressCallback() {
            /** Ignora progresso simples porque este cenário valida os eventos estruturados. */
            @Override
            public void onProgress(Integer percent, SalesVideoStatus status, String message) { }

            /** Guarda reservas e liquidações financeiras emitidas por cena. */
            @Override
            public void onProgress(Integer percent,
                                   SalesVideoStatus status,
                                   String message,
                                   String detailsJson) {
                financialEvents.add(detailsJson);
            }
        };

        ProviderArtifacts artifacts = provider.render(multiSceneJob("10"), profile(), callback);

        assertThat(artifacts.providerJobId())
                .isEqualTo("operations/vega-scene-1,operations/vega-scene-2,operations/vega-scene-3");
        assertThat(artifacts.videoFile().content()).isNotEmpty();
        assertThat(artifacts.metadata())
                .containsEntry("duration_seconds", 24)
                .containsEntry("scene_count", 3)
                .containsEntry("assembled_locally", true)
                .containsEntry("modality", "image_to_video")
                .containsEntry("cost_usd", new java.math.BigDecimal("2.4000"));
        assertThat(financialEvents).hasSize(6);
        assertThat(financialEvents.stream().filter(event -> event.contains("PROVIDER_TASK_ACCEPTED"))).hasSize(3);
        assertThat(financialEvents.stream().filter(event -> event.contains("PROVIDER_TASK_SETTLED"))).hasSize(3);
        assertThat(server.takeRequest().getPath()).isEqualTo("/approved-vega.png");
        for (int scene = 1; scene <= 3; scene++) {
            RecordedRequest create = server.takeRequest();
            assertThat(create.getPath())
                    .isEqualTo("/models/veo-3.1-fast-generate-preview:predictLongRunning");
            assertThat(create.getBody().readUtf8())
                    .contains("Scene " + scene + " of 3")
                    .contains("ação Vega " + scene)
                    .contains("\"image\":{")
                    .contains("\"bytesBase64Encoded\":")
                    .doesNotContain("inlineData")
                    .contains("\"personGeneration\":\"allow_adult\"");
            assertThat(server.takeRequest().getPath()).isEqualTo("/operations/vega-scene-" + scene);
            assertThat(server.takeRequest().getPath()).isEqualTo("/download/vega-scene-" + scene);
        }
    }

    /** Deve bloquear antes do provider quando as cenas excederem o teto aprovado. */
    @Test
    void shouldRejectMultiSceneCostAbovePersistedBudget() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getVeo().setModel("veo-3.1-fast-generate-preview");
        VeoVideoProvider provider = new VeoVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(multiSceneJob("1.00"), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("excede teto");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Deve preservar a causa segura devolvida pelo VEO para orientar correção sem nova tentativa cega. */
    @Test
    void shouldExposeSafeProviderErrorBody() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"durationSeconds must be a string\"}}"));
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("durationSeconds must be a string");
    }

    /** Cria uma resposta JSON para a Gemini API simulada. */
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

    /** Gera um MP4 pequeno e íntegro para exercitar a montagem real por ffmpeg. */
    private byte[] validMp4() throws Exception {
        Path output = Files.createTempFile("veo-provider-test", ".mp4");
        try {
            Process process = new ProcessBuilder(
                    "ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=32x32:d=0.2",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", output.toString())
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            assertThat(process.waitFor()).isZero();
            return Files.readAllBytes(output);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    /** Configura o provider VEO apontando para a API simulada. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getVeo().setEnabled(true);
        properties.getProviders().getVeo().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getVeo().setApiKey("gemini-test-key");
        properties.getProviders().getVeo().setPollInterval(Duration.ofMillis(1));
        return properties;
    }

    /** Cria um job de render VEO. */
    private SalesVideoJob job() {
        return job("VEO");
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
                          "characterImagePrompt": "mulher brasileira elegante, mentora do Metodo MUSA",
                          "characterImageReferenceUrl": "https://assets.example/musa-character.png",
                          "visual_provider_directives": "Very sharp image, crisp focus and constant soft natural daylight"
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria o contrato do Estúdio com três cenas e imagem-base aprovada. */
    private SalesVideoJob multiSceneJob(String budgetLimitUsd) {
        SalesVideoJob base = job("VEO");
        return new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                base.providerName(), base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                """
                        {
                          "videoProductionCycleId": 7,
                          "budgetLimitUsd": %s,
                          "sceneCount": 3,
                          "image_to_video": {
                            "enabled": true,
                            "source_image_url": "%sapproved-vega.png"
                          },
                          "cut_plan": [
                            {"order":1,"role":"HOOK_DOR","visual_objective":"ação Vega 1","continuity_anchor":"mesma personagem"},
                            {"order":2,"role":"MECANISMO","visual_objective":"ação Vega 1","continuity_anchor":"mesma personagem"},
                            {"order":3,"role":"MECANISMO","visual_objective":"ação Vega 2","continuity_anchor":"mesma personagem"},
                            {"order":4,"role":"RESULTADO","visual_objective":"ação Vega 2","continuity_anchor":"mesma personagem"},
                            {"order":5,"role":"PROVA","visual_objective":"ação Vega 3","continuity_anchor":"mesma personagem"},
                            {"order":6,"role":"CTA","visual_objective":"ação Vega 3","continuity_anchor":"mesma personagem"}
                          ]
                        }
                        """.formatted(budgetLimitUsd, server.url("/").toString()),
                base.createdAt(), base.updatedAt());
    }

    /** Cria um perfil com roteiro aprovado para o VEO. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "Script text",
                "Hook",
                "CTA",
                "Caption",
                "[{\"scene\":1}]",
                "OPENAI",
                "gpt-5.2",
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
                8,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}

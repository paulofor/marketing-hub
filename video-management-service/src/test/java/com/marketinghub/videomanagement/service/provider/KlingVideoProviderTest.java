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
import java.nio.charset.StandardCharsets;
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

/** Responsabilidade: validar o adapter direto do Kling no módulo de vídeo. */
class KlingVideoProviderTest {
    private MockWebServer server;

    /** Inicializa a API Kling simulada para capturar criação, polling e download. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API Kling simulada após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve criar uma tarefa Kling text-to-video, aguardar sucesso e devolver MP4 auditável. */
    @Test
    void shouldRenderKlingJobThroughTextToVideoApi() throws Exception {
        server.enqueue(json("""
                {"code":0,"message":"SUCCEED","data":{"task_id":"kling-task-123","task_status":"submitted"}}
                """));
        server.enqueue(json("""
                {
                  "code": 0,
                  "message": "SUCCEED",
                  "data": {
                    "task_id": "kling-task-123",
                    "task_status": "succeed",
                    "task_result": {
                      "videos": [
                        {"url": "%s/download/kling-task-123.mp4"}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        KlingVideoProvider provider = new KlingVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        AtomicInteger progress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job(), profile(),
                (percent, status, message) -> progress.set(percent == null ? 0 : percent));

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("kling-task-123");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-1-kling.mp4");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "KLING_3_0")
                .containsEntry("model", "kling-v2-1-master")
                .containsEntry("duration_seconds", 5);
        assertThat(progress.get()).isEqualTo(95);
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/videos/text2video");
        assertThat(createRequest.getHeader("Authorization")).isEqualTo("Bearer kling-test-key");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"model_name\":\"kling-v2-1-master\"")
                .contains("\"aspect_ratio\":\"9:16\"")
                .contains("MUSA experimento 68")
                .contains("elegante, íntima e prática")
                .contains("No seductive pose")
                .contains("Very sharp image, crisp focus and constant soft natural daylight");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/videos/text2video/kling-task-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/kling-task-123.mp4");
    }

    /** Deve pedir dez segundos e usar somente a cena MECANISMO quando o Estúdio solicita clipe isolado. */
    @Test
    void shouldRenderTenSecondIsolatedStudioScene() throws Exception {
        server.enqueue(json("""
                {"code":0,"message":"SUCCEED","data":{"task_id":"kling-scene-123","task_status":"submitted"}}
                """));
        server.enqueue(json("""
                {
                  "code": 0,
                  "message": "SUCCEED",
                  "data": {
                    "task_id": "kling-scene-123",
                    "task_status": "succeed",
                    "task_result": {"videos": [{"url": "%s/download/kling-scene-123.mp4"}]}
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        KlingVideoProvider provider = new KlingVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(studioSceneJob(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata())
                .containsEntry("duration_seconds", 10)
                .containsEntry("cost_usd", new java.math.BigDecimal("0.40"));
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"duration\":\"10\"")
                .contains("MECANISMO")
                .contains("microajuste visível")
                .doesNotContain("Recognizable pain, plausible mechanism, personal value and CTA");
    }

    /** Deve usar image-to-video quando o job traz imagem aprovada estruturada. */
    @Test
    void shouldRenderKlingJobThroughImageToVideoApiWhenSourceImageExists() throws Exception {
        server.enqueue(json("""
                {"code":0,"message":"SUCCEED","data":{"task_id":"kling-image-task-123","task_status":"submitted"}}
                """));
        server.enqueue(json("""
                {
                  "code": 0,
                  "message": "SUCCEED",
                  "data": {
                    "task_id": "kling-image-task-123",
                    "task_status": "succeed",
                    "task_result": {
                      "videos": [
                        {"url": "%s/download/kling-image-task-123.mp4"}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        KlingVideoProvider provider = new KlingVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(jobWithSourceImage(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata())
                .containsEntry("provider", "KLING_3_0")
                .containsEntry("modality", "image_to_video");
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/videos/image2video");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"model_name\":\"kling-v2-1-master\"")
                .contains("\"image\":\"https://assets.example/musa-approved.png\"");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/videos/image2video/kling-image-task-123");
    }

    /** Deve preservar imagem, duração e ação específica quando a cena isolada usa image-to-video. */
    @Test
    void shouldRenderIsolatedStudioSceneThroughImageToVideoWithoutLosingSceneContract() throws Exception {
        server.enqueue(json("""
                {"code":0,"message":"SUCCEED","data":{"task_id":"kling-studio-image-123","task_status":"submitted"}}
                """));
        server.enqueue(json("""
                {
                  "code": 0,
                  "message": "SUCCEED",
                  "data": {
                    "task_id": "kling-studio-image-123",
                    "task_status": "succeed",
                    "task_result": {"videos": [{"url": "%s/download/kling-studio-image-123.mp4"}]}
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        KlingVideoProvider provider = new KlingVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(
                studioSceneJobWithSourceImage(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata())
                .containsEntry("modality", "image_to_video")
                .containsEntry("duration_seconds", 10);
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/videos/image2video");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"duration\":\"10\"")
                .contains("\"image\":\"https://assets.example/musa-approved.png\"")
                .contains("microajuste visível na manga")
                .doesNotContain("Recognizable pain, plausible mechanism, personal value and CTA");
    }

    /** Deve falhar cedo quando a chave Kling não estiver configurada. */
    @Test
    void shouldRejectMissingKlingApiKey() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getKling().setApiKey("");
        KlingVideoProvider provider = new KlingVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("KLING_API_KEY");
    }

    /** Deve aceitar chave Kling montada em arquivo de secret no container. */
    @Test
    void shouldReadKlingApiKeyFromSecretFile() throws Exception {
        server.enqueue(json("""
                {"code":0,"message":"SUCCEED","data":{"task_id":"kling-task-123","task_status":"submitted"}}
                """));
        server.enqueue(json("""
                {
                  "code": 0,
                  "message": "SUCCEED",
                  "data": {
                    "task_id": "kling-task-123",
                    "task_status": "succeed",
                    "task_result": {
                      "videos": [
                        {"url": "%s/download/kling-task-123.mp4"}
                      ]
                    }
                  }
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        Path keyFile = Files.createTempFile("kling-api-key", ".txt");
        Files.writeString(keyFile, "kling-file-key\n");
        VideoManagementProperties properties = properties();
        properties.getProviders().getKling().setApiKey(null);
        properties.getProviders().getKling().setApiKeyFile(keyFile.toString());
        KlingVideoProvider provider = new KlingVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer kling-file-key");
        Files.deleteIfExists(keyFile);
    }

    /** Deve gerar três cenas distintas, conciliar cada task e montar um único vídeo de trinta segundos. */
    @Test
    void shouldRenderAndAssembleThreeSceneCommercialVideo() throws Exception {
        byte[] clip = validMp4();
        for (int scene = 1; scene <= 3; scene++) {
            String taskId = "kling-scene-" + scene;
            server.enqueue(json("""
                    {"code":0,"message":"SUCCEED","data":{"task_id":"%s","task_status":"submitted"}}
                    """.formatted(taskId)));
            server.enqueue(json("""
                    {
                      "code": 0,
                      "message": "SUCCEED",
                      "data": {
                        "task_id": "%s",
                        "task_status": "succeed",
                        "task_result": {"videos": [{"url": "%s/download/%s.mp4"}]}
                      }
                    }
                    """.formatted(taskId, server.url("/").toString().replaceAll("/$", ""), taskId)));
            server.enqueue(mp4Response(clip));
        }
        KlingVideoProvider provider = new KlingVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        List<String> financialEvents = new ArrayList<>();
        ProgressCallback callback = new ProgressCallback() {
            /** Ignora a mensagem simples porque este teste verifica o contrato estruturado. */
            @Override
            public void onProgress(Integer percent, SalesVideoStatus status, String message) { }

            /** Preserva os eventos financeiros emitidos por cada cena Kling. */
            @Override
            public void onProgress(Integer percent,
                                   SalesVideoStatus status,
                                   String message,
                                   String detailsJson) {
                financialEvents.add(detailsJson);
            }
        };

        ProviderArtifacts artifacts = provider.render(multiSceneJob(), profile(), callback);

        assertThat(artifacts.providerJobId())
                .isEqualTo("kling-scene-1,kling-scene-2,kling-scene-3");
        assertThat(artifacts.videoFile().content()).isNotEmpty();
        assertThat(artifacts.metadata())
                .containsEntry("duration_seconds", 30)
                .containsEntry("scene_count", 3)
                .containsEntry("assembled_locally", true)
                .containsEntry("cost_usd", new java.math.BigDecimal("1.20"));
        assertThat(financialEvents).hasSize(6);
        assertThat(financialEvents.stream().filter(value -> value.contains("PROVIDER_TASK_ACCEPTED"))).hasSize(3);
        assertThat(financialEvents.stream().filter(value -> value.contains("PROVIDER_TASK_SETTLED"))).hasSize(3);

        for (int scene = 1; scene <= 3; scene++) {
            RecordedRequest create = server.takeRequest();
            String body = create.getBody().readUtf8();
            String prompt = new ObjectMapper().readTree(body).path("prompt").asText();
            assertThat(create.getPath()).isEqualTo("/v1/videos/image2video");
            assertThat(body).contains("Scene " + scene + " of 3");
            assertThat(body).contains("cena específica " + scene);
            assertThat(prompt).contains("No embedded text");
            assertThat(prompt.length()).isLessThanOrEqualTo(KlingVideoProvider.MAX_PROMPT_CHARACTERS);
            assertThat(prompt.getBytes(StandardCharsets.UTF_8).length)
                    .isLessThanOrEqualTo(KlingVideoProvider.MAX_PROMPT_UTF8_BYTES);
            assertThat(server.takeRequest().getPath()).isEqualTo("/v1/videos/image2video/kling-scene-" + scene);
            assertThat(server.takeRequest().getPath()).isEqualTo("/download/kling-scene-" + scene + ".mp4");
        }
    }

    /** Cria uma resposta JSON para a API Kling simulada. */
    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    /** Cria uma resposta MP4 mínima com assinatura ftyp. */
    private MockResponse mp4Response() {
        return mp4Response(new byte[] {
                0, 0, 0, 32, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0
        });
    }

    /** Cria uma resposta MP4 com o conteúdo informado pelo cenário de teste. */
    private MockResponse mp4Response(byte[] content) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody(new Buffer().write(content));
    }

    /** Gera um clipe MP4 mínimo e íntegro para exercitar a montagem real pelo ffmpeg. */
    private byte[] validMp4() throws Exception {
        Path output = Files.createTempFile("kling-provider-test", ".mp4");
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

    /** Configura o provider Kling apontando para a API simulada. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getKling().setEnabled(true);
        properties.getProviders().getKling().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getKling().setApiKey("kling-test-key");
        properties.getProviders().getKling().setPollInterval(Duration.ofMillis(1));
        return properties;
    }

    /** Cria um job de render Kling com diretivas visuais anti-sensualização. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "KLING_3_0",
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
                          "assembly_plan": {
                            "scenes": [
                              {"role":"DOR","title":"Dor cotidiana","message":"Ela se arruma, mas sente que falta presença."},
                              {"role":"MECANISMO","title":"Microações MUSA","message":"Reduzir ruído visual, peça-sinal, cor e acabamento."},
                              {"role":"CTA","title":"Diagnóstico gratuito","message":"Veja seu Plano MUSA de 7 dias."}
                            ]
                          },
                          "visual_provider_directives": "Very sharp image, crisp focus and constant soft natural daylight. No seductive pose."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job Kling que representa somente a cena MECANISMO de dez segundos. */
    private SalesVideoJob studioSceneJob() {
        SalesVideoJob base = job();
        return new SalesVideoJob(
                base.id(),
                base.profileId(),
                base.scriptId(),
                base.tenantId(),
                base.providerFamily(),
                base.providerName(),
                base.providerJobId(),
                base.jobType(),
                base.status(),
                base.retryAttempt(),
                base.retryReason(),
                base.retryOfJobId(),
                base.retryNotes(),
                base.progressPercent(),
                base.failureCode(),
                base.failureDetail(),
                base.requestedBy(),
                base.requestedAt(),
                base.startedAt(),
                base.finishedAt(),
                base.expiresAt(),
                base.assetId(),
                base.posterAssetId(),
                base.vttAssetId(),
                """
                        {
                          "generation_strategy": "SCENE_BY_SCENE_MONTAGE",
                          "scene": {
                            "order": 3,
                            "role": "MECANISMO",
                            "prompt": "A mesma mulher usa o celular e executa um microajuste visível na manga.",
                            "duration_seconds": 10
                          },
                          "provider_strategy": {"expected_clip_duration_seconds": 10}
                        }
                        """,
                base.createdAt(),
                base.updatedAt());
    }

    /** Cria um job Kling com URL de imagem aprovada para animação image-to-video. */
    private SalesVideoJob jobWithSourceImage() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "KLING_3_0",
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
                          "image_to_video": {
                            "enabled": true,
                            "source_image_provider": "APPROVED_ASSET",
                            "source_image_asset_id": 1925,
                            "source_image_url": "https://assets.example/musa-approved.png",
                            "animation_provider": "KLING_3_0"
                          },
                          "visual_provider_directives": "Very sharp image, crisp focus and constant soft natural daylight. No seductive pose."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job governado por Plutus com três cenas e cortes distintos do storyboard. */
    private SalesVideoJob multiSceneJob() {
        SalesVideoJob base = jobWithSourceImage();
        return new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                base.providerName(), base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                """
                        {
                          "videoProductionCycleId": 7,
                          "sceneCount": 3,
                          "provider_strategy": {"expected_clip_duration_seconds": 10},
                          "image_to_video": {
                            "enabled": true,
                            "source_image_url": "https://assets.example/musa-approved.png"
                          },
                          "cut_plan": [
                            {"order":1,"duration_seconds":5,"role":"HOOK_DOR","visual_objective":"cena específica 1 %s","continuity_anchor":"mesma personagem"},
                            {"order":2,"duration_seconds":5,"role":"MECANISMO","visual_objective":"cena específica 1","continuity_anchor":"mesma personagem"},
                            {"order":3,"duration_seconds":5,"role":"MECANISMO","visual_objective":"cena específica 2","continuity_anchor":"mesma personagem"},
                            {"order":4,"duration_seconds":5,"role":"RESULTADO","visual_objective":"cena específica 2","continuity_anchor":"mesma personagem"},
                            {"order":5,"duration_seconds":5,"role":"PROVA","visual_objective":"cena específica 3","continuity_anchor":"mesma personagem"},
                            {"order":6,"duration_seconds":5,"role":"CTA","visual_objective":"cena específica 3","continuity_anchor":"mesma personagem"}
                          ]
                        }
                        """.formatted("detalhe visual específico ".repeat(400)),
                base.createdAt(), base.updatedAt());
    }

    /** Cria a combinação real do Estúdio: cena MECANISMO isolada com imagem-base aprovada. */
    private SalesVideoJob studioSceneJobWithSourceImage() {
        SalesVideoJob base = studioSceneJob();
        return new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                base.providerName(), base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                """
                        {
                          "generation_strategy": "SCENE_BY_SCENE_MONTAGE",
                          "scene": {
                            "order": 3,
                            "role": "MECANISMO",
                            "prompt": "A mesma mulher usa o celular e executa um microajuste visível na manga.",
                            "duration_seconds": 10
                          },
                          "provider_strategy": {"expected_clip_duration_seconds": 10},
                          "image_to_video": {
                            "enabled": true,
                            "source_image_provider": "APPROVED_ASSET",
                            "source_image_asset_id": 1953,
                            "source_image_url": "https://assets.example/musa-approved.png",
                            "animation_provider": "KLING_3_0"
                          }
                        }
                        """,
                base.createdAt(), base.updatedAt());
    }

    /** Cria um perfil MUSA com roteiro aprovado. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "Você se arruma, mas sente que falta presença. O Método MUSA cria microações de 7 dias.",
                "Você se arruma e ainda sente que falta presença?",
                "Ver meu plano MUSA de 7 dias",
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
                "HERO",
                "MUSA experimento 68 - Kling anti-sensualizacao funcional",
                "MUSA",
                "elegante, íntima e prática",
                "calorosa e direta",
                "pt-BR",
                30,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}

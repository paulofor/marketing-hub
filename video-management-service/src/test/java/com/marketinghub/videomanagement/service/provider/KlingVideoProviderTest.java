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
                .containsEntry("model", "kling-v3-0")
                .containsEntry("duration_seconds", 5);
        assertThat(progress.get()).isEqualTo(95);
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v1/videos/text2video");
        assertThat(createRequest.getHeader("Authorization")).isEqualTo("Bearer kling-test-key");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"model_name\":\"kling-v3-0\"")
                .contains("\"aspect_ratio\":\"9:16\"")
                .contains("Método MUSA")
                .contains("No seductive pose")
                .contains("Very sharp image, crisp focus and constant soft natural daylight");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/videos/text2video/kling-task-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/kling-task-123.mp4");
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
                .contains("\"model_name\":\"kling-v3-0\"")
                .contains("\"image\":\"https://assets.example/musa-approved.png\"");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/videos/image2video/kling-image-task-123");
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

    /** Cria uma resposta JSON para a API Kling simulada. */
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

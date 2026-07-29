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

/** Responsabilidade: validar o adapter direto da HeyGen no módulo de vídeo. */
class HeyGenVideoProviderTest {
    private MockWebServer server;

    /** Inicializa a API HeyGen simulada para capturar criação, polling e download. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API HeyGen simulada após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve criar uma tarefa HeyGen, aguardar URL final e devolver MP4 auditável. */
    @Test
    void shouldRenderHeyGenJobThroughAvatarVideoApi() throws Exception {
        server.enqueue(json("""
                {"data":{"video_id":"heygen-video-123","status":"pending","output_format":"mp4"}}
                """));
        server.enqueue(json("""
                {
                  "data": {
                    "id": "heygen-video-123",
                    "title": "MUSA",
                    "captioned_video_url": "%s/download/heygen-video-123.mp4",
                    "video_url": "%s/download/raw-heygen-video-123.mp4",
                    "duration": 30.5,
                    "video_page_url": "https://app.heygen.com/video/heygen-video-123"
                  }
                }
                """.formatted(baseUrl(), baseUrl())));
        server.enqueue(mp4Response());
        HeyGenVideoProvider provider = new HeyGenVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        AtomicInteger progress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job(), profile(),
                (percent, status, message) -> progress.set(percent == null ? 0 : percent));

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("heygen-video-123");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-1-heygen.mp4");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "HEYGEN")
                .containsEntry("engine_type", "avatar_iv")
                .containsEntry("aspect_ratio", "9:16");
        assertThat(progress.get()).isEqualTo(95);
        RecordedRequest createRequest = server.takeRequest();
        assertThat(createRequest.getPath()).isEqualTo("/v3/videos");
        assertThat(createRequest.getHeader("X-Api-Key")).isEqualTo("heygen-test-key");
        assertThat(createRequest.getHeader("Idempotency-Key")).isEqualTo("marketing-hub-sales-video-1");
        assertThat(createRequest.getBody().readUtf8())
                .contains("\"avatar_id\":\"avatar-musa\"")
                .contains("\"voice_id\":\"voice-ptbr\"")
                .contains("\"aspect_ratio\":\"9:16\"")
                .contains("\"output_format\":\"mp4\"")
                .contains("\"background\"")
                .contains("\"type\":\"color\"")
                .contains("\"value\":\"#F8F0EA\"")
                .doesNotContain("\"motion_prompt\"");
        assertThat(server.takeRequest().getPath()).isEqualTo("/v3/videos/heygen-video-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/heygen-video-123.mp4");
    }

    /** Deve enviar motion_prompt quando o engine configurado suporta Avatar V. */
    @Test
    void shouldSendMotionPromptForAvatarVEngine() throws Exception {
        server.enqueue(json("""
                {"data":{"video_id":"heygen-video-123","status":"pending","output_format":"mp4"}}
                """));
        server.enqueue(json("""
                {"data":{"id":"heygen-video-123","video_url":"%s/download/heygen-video-123.mp4","duration":30.5}}
                """.formatted(baseUrl())));
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        properties.getProviders().getHeygen().setEngineType("avatar_v");
        HeyGenVideoProvider provider = new HeyGenVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getBody().readUtf8())
                .contains("\"motion_prompt\"")
                .contains("No sensualized pose")
                .contains("Natural presenter");
    }

    /** Deve falhar cedo quando a chave HeyGen não estiver configurada. */
    @Test
    void shouldRejectMissingHeyGenApiKey() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getHeygen().setApiKey("");
        HeyGenVideoProvider provider = new HeyGenVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("HEYGEN_API_KEY");
    }

    /** Deve falhar com orientação clara quando avatar ou voz não estiverem configurados. */
    @Test
    void shouldRequireAvatarAndVoiceConfiguration() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getHeygen().setAvatarId("");
        HeyGenVideoProvider provider = new HeyGenVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(jobWithoutHeyGenIds(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("VIDEO_PROVIDERS_HEYGEN_AVATAR_ID");
    }

    /** Deve aceitar chave HeyGen montada em arquivo de secret no container. */
    @Test
    void shouldReadHeyGenApiKeyFromSecretFile() throws Exception {
        server.enqueue(json("""
                {"data":{"video_id":"heygen-video-123","status":"pending","output_format":"mp4"}}
                """));
        server.enqueue(json("""
                {"data":{"id":"heygen-video-123","video_url":"%s/download/heygen-video-123.mp4","duration":30.5}}
                """.formatted(baseUrl())));
        server.enqueue(mp4Response());
        Path keyFile = Files.createTempFile("heygen-api-key", ".txt");
        Files.writeString(keyFile, "heygen-file-key\n");
        VideoManagementProperties properties = properties();
        properties.getProviders().getHeygen().setApiKey(null);
        properties.getProviders().getHeygen().setApiKeyFile(keyFile.toString());
        HeyGenVideoProvider provider = new HeyGenVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getHeader("X-Api-Key")).isEqualTo("heygen-file-key");
        Files.deleteIfExists(keyFile);
    }

    /** Cria uma resposta JSON para a API HeyGen simulada. */
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

    /** Configura o provider HeyGen apontando para a API simulada. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getHeygen().setEnabled(true);
        properties.getProviders().getHeygen().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getHeygen().setApiKey("heygen-test-key");
        properties.getProviders().getHeygen().setAvatarId("avatar-config");
        properties.getProviders().getHeygen().setVoiceId("voice-config");
        properties.getProviders().getHeygen().setPollInterval(Duration.ofMillis(1));
        return properties;
    }

    /** Cria um job de render HeyGen com IDs específicos e diretivas anti-sensualização. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "HEYGEN",
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
                          "heygen_avatar_id": "avatar-musa",
                          "heygen_voice_id": "voice-ptbr",
                          "visual_provider_directives": "Natural presenter, direct camera, modest clothing, stable light. No sensualized pose."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job HeyGen sem IDs específicos para validar fallback de configuração. */
    private SalesVideoJob jobWithoutHeyGenIds() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "HEYGEN",
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
                "{}",
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
                "MUSA experimento 68 - HeyGen avatar funcional",
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

    /** Retorna a base URL da API simulada sem barra final. */
    private String baseUrl() {
        return server.url("/").toString().replaceAll("/$", "");
    }
}

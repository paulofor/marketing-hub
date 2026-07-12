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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody("mp4-bytes"));
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());
        AtomicInteger progress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job(), profile(),
                (percent, status, message) -> progress.set(percent == null ? 0 : percent));

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.videoFile()).isNotNull();
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.providerJobId()).isEqualTo("operations/video-123");
        assertThat(progress.get()).isEqualTo(95);
        assertThat(server.takeRequest().getPath()).isEqualTo("/models/veo-3.1-generate-preview:predictLongRunning");
        assertThat(server.takeRequest().getPath()).isEqualTo("/operations/video-123");
        assertThat(server.takeRequest().getPath()).isEqualTo("/download/video-123");
    }

    /** Deve aceitar REAL como alias legado para jobs de experimento roteados ao VEO. */
    @Test
    void shouldSupportRealAliasForLegacyExperimentJobs() {
        VeoVideoProvider provider = new VeoVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThat(provider.supports(job("REAL"))).isTrue();
    }

    /** Cria uma resposta JSON para a Gemini API simulada. */
    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
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
                null,
                Instant.now(),
                Instant.now());
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

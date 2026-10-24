package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.marketinghub.videomanagement.client.payload.JobProgressPayload;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;

import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.payload.ProviderPreflightResultPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.VideoJobObservabilityService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP usado pelo worker de vídeo contra o backend. */
class BackendVideoClientTest {
    private MockWebServer server;

    /** Inicializa o backend simulado antes de cada teste de contrato. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado para liberar a porta usada pelo teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve buscar perfil pela rota interna de vídeo, sem depender da rota administrativa com tenant. */
    @Test
    void shouldFetchProfileThroughInternalVideoEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 3,
                          "productId": 63,
                          "videoKind": "HERO",
                          "title": "Video do experimento",
                          "status": "SCRIPT_READY"
                        }
                        """));
        BackendVideoClient client = new BackendVideoClient(
                WebClient.builder(),
                properties(),
                new VideoJobObservabilityService(new SimpleMeterRegistry()));

        SalesVideoProfile profile = client.fetchProfile(3L);

        assertThat(profile.id()).isEqualTo(3L);
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/internal/video/sales-videos/profiles/3");
    }

    /** Deve continuar buscando jobs persistidos quando a reconciliação estiver temporariamente indisponível. */
    @Test
    void shouldFetchPersistedJobsWhenApolloReconciliationFails() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("reconcile unavailable"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("reconcile unavailable"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("reconcile unavailable"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));
        BackendVideoClient client = new BackendVideoClient(
                WebClient.builder(),
                properties(),
                new VideoJobObservabilityService(new SimpleMeterRegistry()));

        assertThat(client.fetchPendingJobs(10)).isEmpty();

        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/sales-videos/autonomy/v1/apollo/reconcile");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/sales-videos/autonomy/v1/apollo/reconcile");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/sales-videos/autonomy/v1/apollo/reconcile");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/internal/video/jobs?status=VIDEO_REQUESTED&limit=10&postProductionContract=PDE_VIDEO_POST_PRODUCTION_V4");
    }

    /** Declara capacidade no claim para impedir consumo por versão antiga durante rollout. */
    @Test
    void declaresRecoveryCapabilityWhenClaiming() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{\"id\":91009}"));
        var client = new BackendVideoClient(WebClient.builder(), properties(),
                new VideoJobObservabilityService(new SimpleMeterRegistry()));
        client.claimJob(91009L, new com.marketinghub.videomanagement.client.payload.JobClaimPayload("fixture", "recuperação"));
        var request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/internal/video/jobs/91009/claim");
        assertThat(request.getBody().readUtf8()).contains("\"postProductionContract\":\"PDE_VIDEO_POST_PRODUCTION_V4\"");
    }

    /** Consome a fila canônica de preflight sem depender de endpoint administrativo ou tenant. */
    @Test
    void shouldFetchPendingProviderPreflightThroughInternalEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "preflightId":31,
                          "cycleId":11,
                          "aggregatorName":"Runway",
                          "accountKey":"RUNWAY_PRIMARY",
                          "productionProfile":"FINAL_CAMPAIGN",
                          "maxCredits":200,
                          "targetDurationSeconds":10,
                          "providerClipDurationSeconds":10,
                          "generationClipCount":1,
                          "aspectRatio":"9:16",
                          "resolution":"720p",
                          "audio":false
                        }]
                        """));
        BackendVideoClient client = client();

        ProviderPreflightJob result = client.fetchPendingProviderPreflight();

        assertThat(result.cycleId()).isEqualTo(11L);
        assertThat(result.accountKey()).isEqualTo("RUNWAY_PRIMARY");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/sales-videos/autonomy/v1/provider-preflight/pending");
    }

    /** Reporta o snapshot sanitizado ao ciclo correto sem expor credencial do agregador. */
    @Test
    void shouldReportProviderPreflightThroughInternalCallback() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        BackendVideoClient client = client();
        ProviderPreflightResultPayload payload = new ProviderPreflightResultPayload(
                "READY",
                "RUNWAY_PRIMARY",
                "final-v1",
                "a".repeat(64),
                "[]",
                "{}",
                "[]",
                "[]",
                new BigDecimal("50"),
                new BigDecimal("500"),
                10000L,
                "{}",
                "{}",
                null,
                null,
                "https://api.dev.runwayml.com/v1/organization",
                Instant.parse("2026-09-03T10:00:00Z"));

        client.reportProviderPreflight(11L, payload);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/sales-videos/autonomy/v1/cycles/11/provider-preflight-result");
        assertThat(request.getBody().readUtf8())
                .contains("\"accountKey\":\"RUNWAY_PRIMARY\"")
                .doesNotContain("runway-test-key");
    }

    /** Recupera confirmação perdida sem alterar job, recibos ou disparar nova geração. */
    @ParameterizedTest
    @ValueSource(longs = {21298L, 93010L})
    void retriesPersistedProgressWhenResponseBodyNeverArrives(long jobId) throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"id\":" + jobId + "}").setBodyDelay(2, TimeUnit.SECONDS));
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"id\":" + jobId + "}"));
        var properties = properties();
        properties.getJobs().setBackendCallTimeout(Duration.ofMillis(300));
        properties.getJobs().setBackendCallBackoff(Duration.ZERO);
        var client = new BackendVideoClient(WebClient.builder(), properties,
                new VideoJobObservabilityService(new SimpleMeterRegistry()));
        var payload = new JobProgressPayload(95, SalesVideoStatus.VIDEO_PROCESSING,
                "Recibos preservados", "{\"voiceReceipt\":\"already-produced\"}");

        assertThat(client.reportProgress(jobId, payload).id()).isEqualTo(jobId);

        var first = server.takeRequest(1, TimeUnit.SECONDS);
        var replay = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(first.getPath()).isEqualTo("/internal/video/jobs/" + jobId + "/progress");
        assertThat(replay.getPath()).isEqualTo(first.getPath());
        assertThat(replay.getBody().readUtf8()).isEqualTo(first.getBody().readUtf8());
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Encerra espera sem resposta após o número finito de tentativas, sem bloquear a fila. */
    @Test
    void exhaustsTimeoutAndPreservesNormalCallback() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            server.enqueue(new MockResponse().setBody("{}").setBodyDelay(2, TimeUnit.SECONDS));
        }
        var properties = properties();
        properties.getJobs().setBackendCallTimeout(Duration.ofMillis(200));
        properties.getJobs().setBackendCallBackoff(Duration.ZERO);
        properties.getJobs().setBackendCallMaxAttempts(2);
        var client = new BackendVideoClient(WebClient.builder(), properties,
                new VideoJobObservabilityService(new SimpleMeterRegistry()));
        var payload = new JobProgressPayload(95, SalesVideoStatus.VIDEO_PROCESSING, "Preservado", "{}");
        long start = System.nanoTime();
        assertThatThrownBy(() -> client.reportProgress(93011L, payload))
                .isInstanceOf(BackendIntegrationException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
        assertThat(server.getRequestCount()).isEqualTo(2);
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"id\":93012}"));
        assertThat(client.reportProgress(93012L, payload).id()).isEqualTo(93012L);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    /** Cria o client sob teste com observabilidade isolada. */
    private BackendVideoClient client() {
        return new BackendVideoClient(
                WebClient.builder(),
                properties(),
                new VideoJobObservabilityService(new SimpleMeterRegistry()));
    }

    /** Configura o client para apontar para o backend simulado. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        return properties;
    }
}

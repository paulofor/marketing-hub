package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.assertThat;

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
                .isEqualTo("/internal/video/jobs?status=VIDEO_REQUESTED&limit=10");
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

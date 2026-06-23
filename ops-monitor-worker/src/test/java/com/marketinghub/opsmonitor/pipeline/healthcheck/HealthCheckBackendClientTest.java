package com.marketinghub.opsmonitor.pipeline.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class HealthCheckBackendClientTest {

    @Test
    void deveEnviarHeartbeatParaBackend() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200));
            server.start();
            var client = new HealthCheckBackendClient(WebClient.builder().baseUrl(server.url("/").toString()).build());

            client.sendHeartbeat(new HealthCheckOutput("backend", java.time.Instant.parse("2026-06-23T12:00:00Z"), "ONLINE", 200, 15, "{}", null)).block();

            var request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/api/internal/ops-monitor/v1/modules/backend/heartbeat");
        }
    }
}

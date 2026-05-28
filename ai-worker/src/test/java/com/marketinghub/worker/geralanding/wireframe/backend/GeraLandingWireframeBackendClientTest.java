package com.marketinghub.worker.geralanding.wireframe.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

class GeraLandingWireframeBackendClientTest {
    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void listPendingExecutionsCallsExperimentScopedWireframeUrl() throws Exception {
        server.enqueue(jsonResponse("[{\"id\":34,\"name\":\"Experimento 34\"}]"));
        server.enqueue(jsonResponse("["
                + "{\"idJob\":\"ef92d7d2-7d84-4b1e-a5a5-ccf3034da4bd\","
                + "\"status\":\"INICIADO\","
                + "\"executionRequestedAt\":\"2026-05-28T16:21:35.330Z\","
                + "\"costUsd\":null}"
                + "]"));

        GeraLandingWireframeBackendClient client = new GeraLandingWireframeBackendClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api",
                new ObjectMapper());

        List<GeraLandingStageExecutionDetailDto> pending = client.listPendingExecutions(20);

        assertThat(server.takeRequest().getPath()).isEqualTo("/api/experiments");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/experiments/34/geralanding/wireframe/stage-executions?includeCompleted=false");
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().experimentId()).isEqualTo(34L);
        assertThat(pending.getFirst().stageCode()).isEqualTo("landing-page-wireframe");
        assertThat(pending.getFirst().idJob()).isEqualTo("ef92d7d2-7d84-4b1e-a5a5-ccf3034da4bd");
        assertThat(pending.getFirst().status()).isEqualTo("INICIADO");
    }

    /** Cria resposta JSON para simular os endpoints reais do backend. */
    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}

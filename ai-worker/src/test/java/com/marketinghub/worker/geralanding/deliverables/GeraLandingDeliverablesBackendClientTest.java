package com.marketinghub.worker.geralanding.deliverables;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client deliverables do GeraLanding. */
class GeraLandingDeliverablesBackendClientTest {

    private MockWebServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Inicializa o backend simulado usado para retornar pendências de deliverables. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada teste do client deliverables. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve aceitar pending grande do GeraLanding sem estourar o buffer padrão de 256 KB. */
    @Test
    void listPendingExecutionsShouldAcceptLargeGeraLandingPayload() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "experimentId": 63,
                            "stageCode": "landing-page-deliverables",
                            "idJob": "job-deliverables-large",
                            "status": "INICIADO",
                            "executionRequestedAt": "2026-07-10T12:00:00Z",
                            "htmlGeraLanding": "%s"
                          }
                        ]
                        """.formatted("x".repeat(350_000))));
        GeraLandingDeliverablesBackendClient client = new GeraLandingDeliverablesBackendClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api",
                1024 * 1024,
                objectMapper);

        List<GeraLandingStageExecutionDetailDto> pending = client.listPendingExecutions(5);

        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/geralanding/deliverables/stage-executions/pending?limit=5");
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().idJob()).isEqualTo("job-deliverables-large");
    }
}

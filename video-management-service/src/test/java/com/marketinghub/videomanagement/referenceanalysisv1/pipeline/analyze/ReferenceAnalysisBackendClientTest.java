package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.math.BigDecimal;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Valida que callbacks de falha preservam auditoria e consumo conhecido no backend. */
class ReferenceAnalysisBackendClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;

    /** Inicializa um backend simulado sem tráfego externo. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Envia tokens e custo junto da falha para que orçamento não seja liberado indevidamente. */
    @Test
    void shouldReportKnownFailureCost() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(server.url("/").uri());
        ReferenceAnalysisBackendClient client =
                new ReferenceAnalysisBackendClient(properties, WebClient.builder());
        ReferenceAnalysisStageContext context = new ReferenceAnalysisStageContext(
                4L,
                1L,
                "default",
                2,
                "producer-4",
                objectMapper.createObjectNode().put("title", "Referência"),
                Instant.now());
        ReferenceAnalysisFailureException failure = new ReferenceAnalysisFailureException(
                "Saída incompleta",
                new IllegalStateException("max_output_tokens"),
                objectMapper.createObjectNode().put("sha256", "abc"),
                objectMapper.createObjectNode().put("request", true),
                objectMapper.createObjectNode().put("status", "incomplete"),
                "gpt-5.6 + gpt-transcribe",
                5465L,
                0L,
                4000L,
                new BigDecimal("0.111722"));

        client.fail(context, failure);

        RecordedRequest request = server.takeRequest();
        JsonNode payload = objectMapper.readTree(request.getBody().readUtf8());
        assertThat(request.getPath()).isEqualTo(
                "/api/internal/sales-videos/reference-analysis/v1/analyze/stage-executions/4/fail");
        assertThat(payload.path("inputTokens").asLong()).isEqualTo(5465L);
        assertThat(payload.path("outputTokens").asLong()).isEqualTo(4000L);
        assertThat(payload.path("costUsd").decimalValue()).isEqualByComparingTo("0.111722");
    }
}

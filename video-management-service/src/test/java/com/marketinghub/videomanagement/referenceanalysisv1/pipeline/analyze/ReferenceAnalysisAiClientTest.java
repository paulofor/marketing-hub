package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.time.Instant;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Valida o contrato multimodal auditável enviado por Apolo à Responses API. */
class ReferenceAnalysisAiClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;

    /** Inicializa uma API OpenAI simulada para impedir consumo externo nos testes. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API simulada ao final de cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Envia imagens, Flex e schema estrito sem autorizar armazenamento remoto da resposta. */
    @Test
    void shouldSendVersionedMultimodalContractWithoutRemoteStorage() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"output\":[],\"usage\":{}}"));
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getReferenceAnalysis().setOpenAiBaseUrl(server.url("/").uri());
        properties.getReferenceAnalysis().setApiKey("openai-test-key");
        ReferenceAnalysisAiClient client = new ReferenceAnalysisAiClient(
                properties, objectMapper, WebClient.builder());
        ReferenceAnalysisStageContext context = new ReferenceAnalysisStageContext(
                91L, 3L, "tenant-test", 1, "producer-91",
                objectMapper.readTree("{\"title\":\"Referência de teste\"}"), Instant.now());
        ObjectNode artifacts = objectMapper.createObjectNode().put("sha256", "abc");
        ReferenceMediaInspector.Evidence evidence = new ReferenceMediaInspector.Evidence(
                artifacts, List.of("data:image/jpeg;base64,AA==", "data:image/jpeg;base64,AQ=="));

        ReferenceAnalysisAiClient.AiInteraction interaction = client.analyze(context, evidence);

        RecordedRequest request = server.takeRequest();
        JsonNode payload = objectMapper.readTree(request.getBody().readUtf8());
        assertThat(request.getPath()).isEqualTo("/responses");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer openai-test-key");
        assertThat(payload.path("model").asText()).isEqualTo("gpt-5.6");
        assertThat(payload.path("service_tier").asText()).isEqualTo("flex");
        assertThat(payload.path("store").asBoolean()).isFalse();
        assertThat(payload.path("input").get(0).path("content")).hasSize(3);
        assertThat(payload.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(payload.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(interaction.request()).isEqualTo(payload);
        assertThat(interaction.response().path("usage").isObject()).isTrue();
    }
}

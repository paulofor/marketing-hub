package com.marketinghub.videomanagement.pdeaudiovisualv1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.AutomaticExecutionControl;
import java.net.URI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: homologar localmente o fluxo BPM completo entre fila, decisão e callback. */
class ApolloPdeAudiovisualFlowTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockWebServer server;

    /** Inicializa o backend simulado e segregado para cada jornada. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado sem deixar recurso de teste ativo. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Conclui Mira ponta a ponta com custo zero e sem declarar avanço local. */
    @Test
    void shouldCompleteMiraWithoutAudiovisualOrProviderCall() throws Exception {
        server.enqueue(pending(false));
        server.enqueue(new MockResponse().setResponseCode(204));

        consumer().processOne();

        var claim = server.takeRequest();
        var callback = server.takeRequest();
        JsonNode payload = objectMapper.readTree(callback.getBody().readUtf8());
        JsonNode result = objectMapper.readTree(payload.path("resultJson").asText());
        JsonNode evidence = objectMapper.readTree(payload.path("evidenceJson").asText());
        assertThat(claim.getPath()).endsWith(
                "?processCode=pde-construction-approval&activityId=audiovisual"
                        + "&executionResourceCode=video-management-service");
        assertThat(callback.getPath()).endsWith("/stage-executions/336/result");
        assertThat(result.path("audiovisualRequirement").asText()).isEqualTo("NOT_REQUIRED");
        assertThat(result.path("artifactIds")).isEmpty();
        assertThat(result.path("providerCalls").asInt()).isZero();
        assertThat(result.path("creditsConsumed").asInt()).isZero();
        assertThat(result.has("nextStageCode")).isFalse();
        assertThat(evidence.path("externalSideEffects").asBoolean()).isFalse();
        assertThat(payload.path("modelUsages")).isEmpty();
        assertThat(payload.path("executionAudit").path("executionMode").asText())
                .isEqualTo("DETERMINISTIC");
    }

    /** Bloqueia um vídeo obrigatório no callback funcional sem consultar provider. */
    @Test
    void shouldBlockRequiredVideoUntilGovernedAuthorizationExists() throws Exception {
        server.enqueue(pending(true));
        server.enqueue(new MockResponse().setResponseCode(204));

        consumer().processOne();

        server.takeRequest();
        var callback = server.takeRequest();
        JsonNode payload = objectMapper.readTree(callback.getBody().readUtf8());
        JsonNode result = objectMapper.readTree(payload.path("resultJson").asText());
        assertThat(callback.getPath()).endsWith("/stage-executions/336/failure");
        assertThat(result.path("audiovisualRequirement").asText())
                .isEqualTo("REQUIRES_AUTHORIZATION");
        assertThat(result.path("providerCalls").asInt()).isZero();
        assertThat(payload.path("blockerGuidance").path("category").asText())
                .isEqualTo("AUTHORIZATION_REQUIRED");
    }

    /** Monta o consumidor real mantendo somente o controle PLAY como test double. */
    private ApolloPdeAudiovisualBpmTaskConsumer consumer() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        properties.getPdeAudiovisual().setEnabled(true);
        properties.getJobs().setBackendCallBackoff(java.time.Duration.ofMillis(1));
        ApolloPdeAudiovisualBackendClient backend =
                new ApolloPdeAudiovisualBackendClient(WebClient.builder(), properties);
        AutomaticExecutionControl control = mock(AutomaticExecutionControl.class);
        when(control.allowsAutomaticExecution()).thenReturn(true);
        return new ApolloPdeAudiovisualBpmTaskConsumer(
                properties,
                backend,
                new ApolloPdeAudiovisualRequirementEvaluator(),
                new ApolloPdeAudiovisualCallbackFactory(objectMapper),
                control);
    }

    /** Simula a resposta real do backend com o contrato mínimo do produto. */
    private MockResponse pending(boolean required) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "taskId":336,
                          "agentKey":"videomaker",
                          "processCode":"pde-construction-approval",
                          "processVersion":6,
                          "activityId":"audiovisual",
                          "activityName":"Produzir audiovisual quando previsto",
                          "sourceReference":"product:10@private-validation-v1",
                          "executionResource":{"resourceCode":"video-management-service"},
                          "taskTarget":{
                            "productId":10,
                            "productInternalName":"Mira",
                            "experienceVersion":"private-validation-v1",
                            "pdeContext":{"harness":{"audiovisualRequired":%s}}
                          },
                          "processContextJson":"{}"
                        }]
                        """.formatted(required));
    }
}

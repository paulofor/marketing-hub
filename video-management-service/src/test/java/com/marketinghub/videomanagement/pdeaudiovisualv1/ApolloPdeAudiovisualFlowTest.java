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
                        + "&executionResourceCode=video-management-service"
                        + "&workerContract=APOLLO_AUDIOVISUAL_V1");
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

    /** Bloqueia pela mesma regra o vídeo vindo da produção criativa após consultar ambas as filas. */
    @Test
    void shouldReachCreativeProductionQueueAndPreserveGovernedVideoGate() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));
        server.enqueue(creativePending(true));
        server.enqueue(new MockResponse().setResponseCode(204));

        consumer().processOne();

        var constructionClaim = server.takeRequest();
        var creativeClaim = server.takeRequest();
        var callback = server.takeRequest();
        JsonNode payload = objectMapper.readTree(callback.getBody().readUtf8());
        JsonNode evidence = objectMapper.readTree(payload.path("evidenceJson").asText());
        assertThat(constructionClaim.getPath()).contains("processCode=pde-construction-approval");
        assertThat(creativeClaim.getPath()).contains("processCode=creative-production-approval");
        assertThat(callback.getPath()).endsWith("/stage-executions/504/failure");
        assertThat(evidence.path("processCode").asText())
                .isEqualTo("creative-production-approval");
        assertThat(evidence.path("contractField").asText())
                .isEqualTo("taskTarget.pdeContext.communicationMaterialization.audiovisualRequired");
        assertThat(evidence.path("contractValue").asBoolean()).isTrue();
        assertThat(evidence.path("providerCalls").asInt()).isZero();
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

    /** Simula a atividade audiovisual criada pelo subprocesso de produção criativa. */
    private MockResponse creativePending(boolean required) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "taskId":504,
                          "agentKey":"videomaker",
                          "processCode":"creative-production-approval",
                          "processVersion":1,
                          "activityId":"audiovisual",
                          "activityName":"Materializar peças audiovisuais quando previstas",
                          "sourceReference":"experiment:93",
                          "executionResource":{"resourceCode":"video-management-service"},
                          "taskTarget":{
                            "productId":10,
                            "productInternalName":"Mira",
                            "experienceVersion":"private-v3",
                            "pdeContext":{"contractVersion":"APOLLO_COMMUNICATION_AUDIOVISUAL_INPUT_V1",
                              "communicationMaterialization":{"audiovisualRequired":%s}}
                          },
                          "processContextJson":"{}"
                        }]
                        """.formatted(required));
    }
}

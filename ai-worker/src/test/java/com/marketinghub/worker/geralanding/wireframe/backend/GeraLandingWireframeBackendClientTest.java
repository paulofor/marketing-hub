package com.marketinghub.worker.geralanding.wireframe.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsável por validar o contrato HTTP do client backend da etapa wireframe.
 */
class GeraLandingWireframeBackendClientTest {
    private MockWebServer server;

    /** Inicializa o servidor HTTP simulado usado nos testes do client. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o servidor HTTP simulado ao final de cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve consultar apenas a fila interna pending de wireframe e preservar JSON estruturado. */
    @Test
    void listPendingExecutionsCallsInternalWireframeQueueAndKeepsStructuredJson() throws Exception {
        server.enqueue(jsonResponse("["
                + "{\"experimentId\":34,"
                + "\"jobid\":\"ef92d7d2-7d84-4b1e-a5a5-ccf3034da4bd\","
                + "\"stageCode\":\"landing-page-wireframe\","
                + "\"experiment\":{"
                + "\"id\":34,"
                + "\"name\":\"Personal trainers\","
                + "\"campaignAngle\":{\"primaryPromise\":\"Agenda cheia\"},"
                + "\"adCopy\":{\"headline\":\"Sem desconto\"},"
                + "\"adImageBriefing\":{\"concept\":\"Calendário lotado\"},"
                + "\"landingPageWireframe\":null"
                + "},"
                + "\"hypothesis\":{"
                + "\"framework\":{"
                + "\"pain\":{\"summary\":\"agenda vazia\"},"
                + "\"result\":{\"summary\":\"agenda cheia\"}"
                + "}"
                + "}"
                + "}"
                + "]"));

        GeraLandingWireframeBackendClient client = new GeraLandingWireframeBackendClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api",
                new ObjectMapper());

        List<GeraLandingStageExecutionDetailDto> pending = client.listPendingExecutions(20);

        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/geralanding/wireframe/stage-executions/pending");
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().experimentId()).isEqualTo(34L);
        assertThat(pending.getFirst().stageCode()).isEqualTo("landing-page-wireframe");
        assertThat(pending.getFirst().idJob()).isEqualTo("ef92d7d2-7d84-4b1e-a5a5-ccf3034da4bd");
        assertThat(pending.getFirst().status()).isEqualTo("INICIADO");
        assertThat(pending.getFirst().promptData())
                .containsKeys(
                        "campaignAngle",
                        "adCopy",
                        "adImageBriefing",
                        "landingPageWireframe",
                        "NICHE_NAME",
                        "PAIN_JSON",
                        "RESULT_JSON");
        assertThat(pending.getFirst().promptData().get("campaignAngle"))
                .isInstanceOfSatisfying(
                        java.util.Map.class,
                        value -> assertThat(value).containsEntry("primaryPromise", "Agenda cheia"));
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Deve enviar falhas da etapa wireframe para o mesmo callback recebe-resposta usado no sucesso. */
    @Test
    void receiveFailureShouldCallRecebeRespostaWithErrorJson() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        GeraLandingWireframeBackendClient client = new GeraLandingWireframeBackendClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api",
                new ObjectMapper());

        client.receiveFailure(
                "bbed57d0-dcc7-40ab-b936-20a19e21c7fe",
                44L,
                "landing-page-wireframe",
                "Falha OpenAI",
                "Stack trace resumido");

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/wireframe/stage-executions/bbed57d0-dcc7-40ab-b936-20a19e21c7fe/recebe-resposta");
        Map<String, Object> payload = new ObjectMapper().readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("experimentId", 44)
                .containsEntry("stageCode", "landing-page-wireframe")
                .containsEntry("errorMessage", "Falha OpenAI")
                .containsEntry("errorDetail", "Stack trace resumido");
        assertThat(payload).containsKeys("modelResponse", "inputTokens", "outputTokens", "costUsd", "openAiJobId");
    }

    /** Cria resposta JSON para simular os endpoints reais do backend. */
    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}

package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: comprovar auditoria e tentativas limitadas sem consumo de IA externa. */
class ApolloPlanningAiClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<JsonNode> events = new ArrayList<>();
    private final List<Long> waits = new ArrayList<>();
    private MockWebServer server;
    private ApolloPlanningAiClient client;
    private JsonNode request;

    /** Inicializa somente fornecedor HTTP local e credencial sintética. */
    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        var properties = new VideoManagementProperties();
        properties.getApolloPlanner().setOpenAiBaseUrl(server.url("/").uri());
        properties.getApolloPlanner().setApiKey("synthetic-test-secret");
        client = new ApolloPlanningAiClient(properties, WebClient.builder(), waits::add);
        request = mapper.readTree("{\"model\":\"gpt-5.6-sol\",\"service_tier\":\"flex\",\"input\":\"teste segregado\"}");
    }

    /** Encerra o fornecedor sintético após cada cenário. */
    @AfterEach
    void cleanup() throws Exception {
        server.shutdown();
    }

    /** Rejeição temporária repete o mesmo request e preserva ambas as respostas e o intervalo. */
    @Test
    void temporaryRejectionThenSuccessPreservesFlexAndAudit() throws Exception {
        server.enqueue(error(429, "resource_unavailable").setHeader("Retry-After", "12").setHeader("x-request-id", "rejected-local"));
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setHeader("x-request-id", "success-local").setBody("{\"output\":[],\"usage\":{\"input_tokens\":1}}"));
        var result = client.plan(91001L, request, events::add);
        assertThat(result.path("usage").path("input_tokens").asInt()).isEqualTo(1);
        assertThat(waits).containsExactly(12000L);
        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(mapper.readTree(server.takeRequest().getBody().readUtf8())).isEqualTo(request);
        assertThat(mapper.readTree(server.takeRequest().getBody().readUtf8())).isEqualTo(request);
        assertThat(events.stream().filter(e -> "REJECTED".equals(e.path("status").asText())))
                .singleElement().satisfies(e -> {
                    assertThat(e.path("requestId").asText()).isEqualTo("rejected-local");
                    assertThat(e.path("rawResponse").asText()).contains("resource_unavailable");
                    assertThat(e.path("request")).isEqualTo(request);
                });
        assertThat(events.getLast().path("requestId").asText()).isEqualTo("success-local");
        assertThat(events.toString()).doesNotContain("synthetic-test-secret");
    }

    /** Três rejeições encerram a tentativa sem alterar modalidade ou gerar outro ciclo. */
    @Test
    void repeatedTemporaryFailureStopsAfterThreeRequests() {
        for (int i = 0; i < 3; i++) server.enqueue(error(429, "rate_limit_exceeded"));
        assertFailure("APOLLO_PLANNING_UNAVAILABLE");
        assertThat(server.getRequestCount()).isEqualTo(3);
        assertThat(waits).hasSize(2);
        assertThat(waits.get(0)).isBetween(5000L, 5999L);
        assertThat(waits.get(1)).isBetween(10000L, 10999L);
    }

    /** Quota, autenticação, contratos e resposta desconhecida nunca disparam repetição automática. */
    @ParameterizedTest
    @CsvSource({
        "429,insufficient_quota,APOLLO_PLANNING_ACCOUNT_BLOCKED",
        "429,credit_balance_exhausted,APOLLO_PLANNING_ACCOUNT_BLOCKED",
        "429,project_spend_limit_exceeded,APOLLO_PLANNING_ACCOUNT_BLOCKED",
        "429,organization_spend_limit_exceeded,APOLLO_PLANNING_ACCOUNT_BLOCKED",
        "429,organization_usage_limit_exceeded,APOLLO_PLANNING_ACCOUNT_BLOCKED",
        "429,unknown,APOLLO_PLANNING_HTTP_ERROR",
        "400,invalid_request,APOLLO_PLANNING_HTTP_ERROR",
        "401,invalid_api_key,APOLLO_PLANNING_AUTH_ERROR",
        "403,forbidden,APOLLO_PLANNING_AUTH_ERROR",
        "500,server_error,APOLLO_PLANNING_HTTP_ERROR"
    })
    void permanentOrUnclassifiedErrorIsAuditedWithoutRetry(int status, String code, String expected) {
        server.enqueue(error(status, code));
        assertFailure(expected);
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(waits).isEmpty();
        assertThat(events.getLast().path("rawResponse").asText()).contains(code);
        assertThat(events.getLast().path("httpStatus").asInt()).isEqualTo(status);
    }

    /** Espera superior ao limite não é encurtada para antecipar outra chamada. */
    @Test
    void longServerDelayStopsWithoutEarlyRetry() {
        server.enqueue(error(429, "resource_unavailable").setHeader("Retry-After", "120"));
        assertFailure("APOLLO_PLANNING_UNAVAILABLE");
        assertThat(waits).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Aceita a data HTTP e recusa cabeçalho inválido ou superior à janela local. */
    @Test
    void retryAfterDatesPreserveMinimumServerDelay() {
        Instant now = Instant.parse("2026-09-14T03:00:00Z");
        assertThat(ApolloPlanningAiClient.retryDelay(429, "slow_down", "Mon, 14 Sep 2026 03:00:30 GMT", 1, now)).isEqualTo(30000L);
        assertThat(ApolloPlanningAiClient.retryDelay(429, "slow_down", "Mon, 14 Sep 2026 03:02:00 GMT", 1, now)).isNull();
        assertThat(ApolloPlanningAiClient.retryDelay(429, "slow_down", "not-a-delay", 1, now)).isNull();
    }

    /** Corpo inválido continua auditado e bloqueia o provider em vez de virar plano vazio. */
    @Test
    void invalidSuccessfulBodyIsAuditedBeforeValidation() {
        server.enqueue(new MockResponse().setBody("invalid-body"));
        assertFailure("APOLLO_PLANNING_RESPONSE_INVALID");
        assertThat(events.getLast().path("rawResponse").asText()).isEqualTo("invalid-body");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Perda da resposta não repete silenciosamente o POST nem a geração potencialmente aceita. */
    @Test
    void uncertainTransportDoesNotRepeatPost() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        assertFailure("APOLLO_PLANNING_TRANSPORT_ERROR");
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(waits).isEmpty();
        assertThat(events.getLast().path("status").asText()).isEqualTo("TRANSPORT_OR_AUDIT_ERROR");
    }

    /** Recusa resposta que ecoe o segredo, preservando somente o corpo com credencial removida. */
    @Test
    void echoedCredentialIsRedactedFromAudit() {
        server.enqueue(error(401, "synthetic-test-secret"));
        assertFailure("APOLLO_PLANNING_AUTH_ERROR");
        assertThat(events.toString()).doesNotContain("synthetic-test-secret").contains("CREDENCIAL_REMOVIDA");
    }

    /** Falha de persistência anterior ao envio impede qualquer chamada externa. */
    @Test
    void missingRequestAuditPreventsHttpCall() {
        assertThatThrownBy(() -> client.plan(91001L, request, e -> { throw new IllegalStateException("audit unavailable"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    /** Cria somente uma rejeição de fornecedor sintético. */
    private MockResponse error(int status, String code) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"code\":\"" + code + "\",\"message\":\"Resposta sintética\"}}");
    }

    /** Confere o motivo operacional sem permitir conclusão técnica em caso de falha. */
    private void assertFailure(String code) {
        assertThatThrownBy(() -> client.plan(91001L, request, events::add))
                .isInstanceOfSatisfying(VideoProviderException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}

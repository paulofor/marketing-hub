package com.marketinghub.worker.openai.core.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.OpenAiHttpException;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar observabilidade e contrato do client OpenAI Responses API do core. */
class ResponsesApiOpenAiClientTest {

    private MockWebServer server;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private Level originalLevel;

    /** Inicializa servidor HTTP e captura de logs para cada cenário de teste do client OpenAI. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        logger = (Logger) LoggerFactory.getLogger(ResponsesApiOpenAiClient.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    /** Encerra recursos externos e remove o appender de log após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
        server.shutdown();
    }

    /** Deve registrar o request final em modo flex quando a Responses API rejeitar a chamada com erro HTTP. */
    @Test
    void dispatchShouldLogFlexOpenAiRequestWhenHttpErrorHappens() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"Invalid schema\"}}"));
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties("gpt-test"));
        String requestBodyJson = "{\"model\":\"gpt-test\",\"input\":\"Prompt\"}";
        OpenAiRequest request = new OpenAiRequest(
                "gpt-test",
                "Prompt",
                requestBodyJson,
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-123"));

        Throwable thrown = catchThrowable(() -> client.dispatch(request));

        Map<String, Object> sentPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        assertThat(sentPayload)
                .containsEntry("model", "gpt-test")
                .containsEntry("input", "Prompt")
                .containsEntry("service_tier", "flex");
        assertThat(thrown).isInstanceOf(OpenAiHttpException.class);
        assertThat(thrown.getMessage())
                .contains("OpenAI Responses API returned HTTP 400")
                .contains("Invalid schema");
        assertThat(((OpenAiHttpException) thrown).responseBody()).contains("Invalid schema");
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .contains("Falha HTTP na OpenAI Responses API")
                        .contains("jobId=job-123")
                        .contains("\"service_tier\":\"flex\"")
                        .contains("Invalid schema"));
    }

    /** Deve aplicar o service_tier informado pela etapa no payload enviado e no despacho auditável. */
    @Test
    void dispatchShouldApplyRequestServiceTierInOpenAiPayloadAndDispatch() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "resp-test",
                          "output": [
                            {
                              "type": "message",
                              "content": [
                                {"type": "output_text", "text": "{\\"ok\\":true}"}
                              ]
                            }
                          ],
                          "usage": {"input_tokens": 10, "output_tokens": 5}
                        }
                        """));
        enqueuePricingCatalog("gpt-test", "2.00", "8.00", "1.00", "4.00");
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties("gpt-test"));
        OpenAiRequest request = new OpenAiRequest(
                "gpt-test",
                "Prompt",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt\",\"service_tier\":\"default\"}",
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-123"),
                "default");

        var dispatch = client.dispatch(request);
        var result = client.awaitResult(dispatch);

        Map<String, Object> sentPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        assertThat(sentPayload)
                .containsEntry("model", "gpt-test")
                .containsEntry("input", "Prompt")
                .containsEntry("service_tier", "default");
        Map<String, Object> dispatchPayload = new ObjectMapper().readValue(
                dispatch.requestBodyJson(),
                new TypeReference<>() {});
        assertThat(dispatchPayload).containsEntry("service_tier", "default");
        assertThat(result.costUsd()).isEqualByComparingTo(new BigDecimal("0.00006000"));
    }

    /** Deve tentar Flex duas vezes e usar Standard na terceira tentativa após falhas transitórias. */
    @Test
    void dispatchShouldFallbackToStandardOnThirdTransientOpenAiAttempt() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"too many requests\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"still busy\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "resp-retry",
                          "output_text": "{\\"ok\\":true}",
                          "usage": {"input_tokens": 10, "output_tokens": 5}
                        }
                        """));
        enqueuePricingCatalog("gpt-test", "2.00", "8.00", "1.00", "4.00");
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties("gpt-test"),
                delay -> {});
        OpenAiRequest request = new OpenAiRequest(
                "gpt-test",
                "Prompt",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt\"}",
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-retry"));

        var dispatch = client.dispatch(request);
        var result = client.awaitResult(dispatch);

        assertThat(dispatch.openAiJobId()).isEqualTo("resp-retry");
        assertThat(result.modelResponse()).isEqualTo("{\"ok\":true}");
        assertThat(server.getRequestCount()).isEqualTo(4);
        Map<String, Object> firstAttemptPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        Map<String, Object> secondAttemptPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        Map<String, Object> thirdAttemptPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        server.takeRequest();
        assertThat(firstAttemptPayload).containsEntry("service_tier", "flex");
        assertThat(secondAttemptPayload).containsEntry("service_tier", "flex");
        assertThat(thirdAttemptPayload).containsEntry("service_tier", "default");
        Map<String, Object> dispatchPayload = new ObjectMapper().readValue(
                dispatch.requestBodyJson(),
                new TypeReference<>() {});
        assertThat(dispatchPayload).containsEntry("service_tier", "default");
        assertThat(result.costUsd()).isEqualByComparingTo(new BigDecimal("0.00006000"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .contains("Falha HTTP transitória na OpenAI Responses API")
                        .contains("jobId=job-retry")
                        .contains("status=429")
                        .contains("attempt=1"));
    }

    /** Deve estimar custo em modo flex pelo modelo do request quando a configuração não informa tarifa explícita. */
    @Test
    void dispatchShouldEstimateFlexCostFromRequestModelWhenPricingPropertiesAreUnset() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "resp-cost",
                          "output": [
                            {
                              "type": "message",
                              "content": [
                                {"type": "output_text", "text": "{\\"ok\\":true}"}
                              ]
                            }
                          ],
                          "usage": {"input_tokens": 1000000, "output_tokens": 1000000}
                        }
                        """));
        enqueuePricingCatalog("gpt-5.4", "2.50", "15.00", "1.25", "7.50");
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties("gpt-5.4"));
        OpenAiRequest request = new OpenAiRequest(
                "gpt-5.4",
                "Prompt",
                "{\"model\":\"gpt-5.4\",\"input\":\"Prompt\"}",
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-cost"));

        var dispatch = client.dispatch(request);
        var result = client.awaitResult(dispatch);

        assertThat(result.inputTokens()).isEqualTo(1000000);
        assertThat(result.outputTokens()).isEqualTo(1000000);
        assertThat(result.costUsd()).isEqualByComparingTo(new BigDecimal("8.75000000"));
    }

    /** Deve falhar quando o modelo efetivo não existir no catálogo persistido do backend. */
    @Test
    void dispatchShouldFailWhenRequestModelIsMissingFromBackendPricingCatalog() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "resp-missing-cost",
                          "output_text": "{\\"ok\\":true}",
                          "usage": {"input_tokens": 1000, "output_tokens": 2000}
                        }
                        """));
        enqueuePricingCatalog("gpt-other", "20", "40", "10", "20");
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties("gpt-test"));
        OpenAiRequest request = new OpenAiRequest(
                "gpt-test",
                "Prompt",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt\"}",
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-missing-cost"));

        Throwable thrown = catchThrowable(() -> client.dispatch(request));

        assertThat(thrown)
                .isInstanceOf(StageWorkerException.class)
                .hasMessageContaining("Modelo OpenAI não encontrado no catálogo persistido")
                .hasMessageContaining("gpt-test");
    }

    /** Monta propriedades OpenAI apontando o catálogo de preços para o mock do backend. */
    private OpenAiClientProperties properties(String model) {
        return new OpenAiClientProperties(
                "test-key",
                server.url("/").toString(),
                model,
                Duration.ofSeconds(5),
                server.url("/api/modelos/openai/catalogo/v1/modelos").toString(),
                true);
    }

    /** Simula o endpoint backend que lista os modelos persistidos na tabela openai_model. */
    private void enqueuePricingCatalog(
            String code,
            String inputStandardPrice,
            String outputStandardPrice,
            String inputBatchPrice,
            String outputBatchPrice) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "code": "%s",
                            "priceInputStandard": %s,
                            "priceOutputStandard": %s,
                            "priceInputBatch": %s,
                            "priceOutputBatch": %s
                          }
                        ]
                        """.formatted(code, inputStandardPrice, outputStandardPrice, inputBatchPrice, outputBatchPrice)));
    }

}

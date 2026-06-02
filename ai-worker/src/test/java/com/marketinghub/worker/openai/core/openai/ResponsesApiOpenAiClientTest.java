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
                new OpenAiClientProperties(
                        "test-key",
                        server.url("/").toString(),
                        "gpt-test",
                        Duration.ofSeconds(5),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true));
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

    /** Deve fixar service_tier flex no payload enviado e no despacho auditável do core OpenAI. */
    @Test
    void dispatchShouldForceFlexServiceTierInOpenAiPayloadAndDispatch() throws Exception {
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
        ResponsesApiOpenAiClient client = new ResponsesApiOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                new OpenAiClientProperties(
                        "test-key",
                        server.url("/").toString(),
                        "gpt-test",
                        Duration.ofSeconds(5),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true));
        OpenAiRequest request = new OpenAiRequest(
                "gpt-test",
                "Prompt",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt\",\"service_tier\":\"default\"}",
                "schema-wireframe",
                "{\"type\":\"object\"}",
                "# Prompt markdown bruto",
                Map.of("idJob", "job-123"));

        var dispatch = client.dispatch(request);

        Map<String, Object> sentPayload = new ObjectMapper().readValue(
                server.takeRequest().getBody().readUtf8(),
                new TypeReference<>() {});
        assertThat(sentPayload)
                .containsEntry("model", "gpt-test")
                .containsEntry("input", "Prompt")
                .containsEntry("service_tier", "flex");
        Map<String, Object> dispatchPayload = new ObjectMapper().readValue(
                dispatch.requestBodyJson(),
                new TypeReference<>() {});
        assertThat(dispatchPayload).containsEntry("service_tier", "flex");
    }
}

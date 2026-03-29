package com.marketinghub.worker.hypothesisframework;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class HypothesisFrameworkOpenAiClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void fillsMissingJsonSchemaNameForResponsesApi() {
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        HypothesisFrameworkOpenAiClient client = new HypothesisFrameworkOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(requestPayload)),
                MAPPER,
                "test-key",
                "http://openai");

        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAIN",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [{"role": "user", "content": [{"type":"input_text","text":"Teste"}]}],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object"
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = requestPayload.get();
        assertThat(payload).isNotNull();
        Map<String, Object> text = castMap(payload.get("text"));
        Map<String, Object> format = castMap(text.get("format"));
        assertThat(format).containsEntry("name", "hypothesis_framework_pain");
    }

    @Test
    void keepsExistingJsonSchemaName() {
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        HypothesisFrameworkOpenAiClient client = new HypothesisFrameworkOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(requestPayload)),
                MAPPER,
                "test-key",
                "http://openai");

        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BENEFIT",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [{"role": "user", "content": [{"type":"input_text","text":"Teste"}]}],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "name": "already_set",
                              "schema": {
                                "type": "object"
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = requestPayload.get();
        Map<String, Object> text = castMap(payload.get("text"));
        Map<String, Object> format = castMap(text.get("format"));
        assertThat(format).containsEntry("name", "already_set");
    }

    @Test
    void addsMissingPropertiesToRequiredInJsonSchema() {
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        HypothesisFrameworkOpenAiClient client = new HypothesisFrameworkOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(requestPayload)),
                MAPPER,
                "test-key",
                "http://openai");

        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAIN",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [{"role": "user", "content": [{"type":"input_text","text":"Teste"}]}],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "emotional": {"type": "string"},
                                  "social": {"type": "string"},
                                  "financial": {"type": "string"}
                                },
                                "required": ["emotional", "financial"],
                                "additionalProperties": false
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = requestPayload.get();
        Map<String, Object> text = castMap(payload.get("text"));
        Map<String, Object> format = castMap(text.get("format"));
        Map<String, Object> schema = castMap(format.get("schema"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertThat(required).containsExactlyInAnyOrderElementsOf(Set.of("emotional", "social", "financial"));
    }

    @Test
    void prependsCommercialFrameworkInstructionsToPromptText() {
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        HypothesisFrameworkOpenAiClient client = new HypothesisFrameworkOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(requestPayload)),
                MAPPER,
                "test-key",
                "http://openai");

        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OFFER",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [{"role": "user", "content": [{"type":"input_text","text":"Prompt original"}]}],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object"
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = requestPayload.get();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> input = (List<Map<String, Object>>) payload.get("input");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) input.get(0).get("content");
        assertThat(content.get(0).get("text"))
                .asString()
                .startsWith("Você está preenchendo campos de um framework comercial para o Marketing Hub.");
        assertThat(content.get(0).get("text")).asString().contains("Prompt original");
    }

    @Test
    void prepareRequestPayloadForLogIncludesCommercialFrameworkInstructions() {
        HypothesisFrameworkOpenAiClient client = new HypothesisFrameworkOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>())),
                MAPPER,
                "test-key",
                "http://openai");

        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAIN",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [{"role": "system", "content": "Prompt base"}],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object"
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        String preparedPayload = client.prepareRequestPayloadForLog(job);

        assertThat(preparedPayload).contains("Você está preenchendo campos de um framework comercial para o Marketing Hub.");
        assertThat(preparedPayload).contains("Prompt base");
    }

    private ExchangeFunction capturePayloadExchange(AtomicReference<Map<String, Object>> capturedPayload) {
        return request -> {
            MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
            request.body().insert(httpRequest, BODY_INSERTER_CONTEXT).block();
            String requestBody = httpRequest.getBodyAsString().block();
            try {
                capturedPayload.set(MAPPER.readValue(requestBody, new TypeReference<>() {
                }));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"result\\\":\\\"ok\\\"}\"}]}]}")
                    .build());
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static final BodyInserter.Context BODY_INSERTER_CONTEXT = new BodyInserter.Context() {
        private final List<HttpMessageWriter<?>> messageWriters = ExchangeStrategies.withDefaults().messageWriters();

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return messageWriters;
        }

        @Override
        public Optional<ServerHttpRequest> serverRequest() {
            return Optional.empty();
        }

        @Override
        public Map<String, Object> hints() {
            return Collections.emptyMap();
        }
    };
}

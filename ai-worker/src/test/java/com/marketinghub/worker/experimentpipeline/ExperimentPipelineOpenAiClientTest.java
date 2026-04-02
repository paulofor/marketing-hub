package com.marketinghub.worker.experimentpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
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

class ExperimentPipelineOpenAiClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void prependsGlobalRulesAndCampaignAngleGuidanceForCampaignAngleSection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                10L,
                "campaign-angle",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "system", "content": "System"},
                            {"role": "user", "content": "Prompt original de angulo"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(1).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt original de angulo");
        assertThat(userPrompt).contains("Crie a base estratégica de uma campanha Meta Ads + landing page para este produto.");
        assertThat(userPrompt).contains("primaryPromise,");
        assertThat(userPrompt).contains("proofSummary,");
        assertThat(userPrompt).contains("singleMindedPromise,");
        assertThat(userPrompt).contains("primaryCTA,");
        assertThat(userPrompt).contains("landingMatchLine,");
    }

    @Test
    void prependsGlobalRulesOnlyForNonCampaignSections() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                10L,
                "ad-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt de anuncio");
        assertThat(userPrompt).doesNotContain("proofSummary,");
    }

    @Test
    void prependsLandingCopyGuidanceForLandingCopySection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                11L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt da landing");
        assertThat(userPrompt).contains("Objetivo da landing:");
        assertThat(userPrompt).contains("messageMatchSource,");
        assertThat(userPrompt).contains("landingCurta {");
        assertThat(userPrompt).contains("landingCompleta {");
        assertThat(userPrompt).contains("objectionHandlingSection");
        assertThat(userPrompt).contains("closingCTA");
    }

    @Test
    void prependsLandingLayoutGuidanceForLandingLayoutSection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                12L,
                "landing-page-wireframe",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt do wireframe"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt do wireframe");
        assertThat(userPrompt).contains("variantLayoutId");
        assertThat(userPrompt).contains("form-first");
        assertThat(userPrompt).contains("proof-first");
        assertThat(userPrompt).contains("mobilePriorityScore");
        assertThat(userPrompt).contains("dropOffRisk");
        assertThat(userPrompt).contains("sectionDependsOn");
    }

    @Test
    void normalizesJsonSchemaRequiredForStrictResponsesApiCompatibility() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                13L,
                "ad-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "additionalProperties": false,
                                "properties": {
                                  "adCopy": {
                                    "type": "object",
                                    "additionalProperties": false,
                                    "properties": {
                                      "primaryText": {"type": "string"},
                                      "headline": {"type": "string"}
                                    },
                                    "required": ["headline"]
                                  }
                                },
                                "required": ["adCopy"]
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        assertThat(format.get("name")).isEqualTo("experiment_pipeline_ad_copy");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> adCopy = (Map<String, Object>) properties.get("adCopy");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) adCopy.get("required");
        assertThat(required).contains("headline", "primaryText");
    }

    @Test
    void enforcesAdditionalPropertiesFalseForNestedObjectSchemas() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                15L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "landingPageCopy": {
                                    "type": "object",
                                    "properties": {
                                      "messageMatchSource": {"type": "string"}
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageCopy = (Map<String, Object>) properties.get("landingPageCopy");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(landingPageCopy.get("additionalProperties")).isEqualTo(false);
    }

    @Test
    void enforcesGpt52ModelForEveryPipelineCall() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                14L,
                "ad-copy",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        assertThat(payload.get("model")).isEqualTo("gpt-5.2");
    }

    private ExchangeFunction capturePayloadExchange(AtomicReference<Map<String, Object>> payloadRef) {
        return request ->
                readBodyAsString(request.body())
                        .flatMap(body -> {
                            try {
                                payloadRef.set(MAPPER.readValue(body, new TypeReference<>() {}));
                            } catch (Exception ex) {
                                return Mono.error(ex);
                            }
                            String responseBody = """
                                    {
                                      "id": "resp_test",
                                      "status": "completed",
                                      "output": [{
                                        "type": "message",
                                        "content": [{"type": "output_text", "text": "{\\"content\\":\\"ok\\"}"}]
                                      }],
                                      "usage": {"input_tokens": 100, "output_tokens": 20, "total_tokens": 120}
                                    }
                                    """;
                            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .body(responseBody)
                                    .build();
                            return Mono.just(response);
                        });
    }

    @SuppressWarnings("unchecked")
    private <T extends org.springframework.http.ReactiveHttpOutputMessage> Mono<String> readBodyAsString(BodyInserter<?, ? super T> bodyInserter) {
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(org.springframework.http.HttpMethod.POST, "http://localhost");
        BodyInserter.Context context = new BodyInserter.Context() {
            @Override
            public java.util.List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public java.util.Optional<ServerHttpRequest> serverRequest() {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Map<String, Object> hints() {
                return java.util.Collections.emptyMap();
            }
        };

        return ((BodyInserter<Object, T>) bodyInserter)
                .insert((T) mockRequest, context)
                .then(Mono.defer(() ->
                        mockRequest.getBodyAsString().defaultIfEmpty("")));
    }
}

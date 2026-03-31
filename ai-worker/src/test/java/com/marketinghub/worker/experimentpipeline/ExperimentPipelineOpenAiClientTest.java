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
        assertThat(userPrompt).contains("campaignAngle,");
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
        assertThat(userPrompt).doesNotContain("campaignAngle,");
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
        assertThat(userPrompt).contains("heroTitle,");
        assertThat(userPrompt).contains("closingCTA");
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

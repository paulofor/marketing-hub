package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobCompletionPayload;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GeraLandingOpenAiBatchClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingOpenAiBatchClient.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;

    public GeraLandingOpenAiBatchClient(WebClient.Builder builder,
                                        ObjectMapper objectMapper,
                                        @Value("${openai.api-key:}") String apiKey,
                                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de gera-landing ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ExperimentPipelineJobCompletionPayload generate(ExperimentPipelineJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {});
            OpenAiResponse response = callOpenAi(payload);
            String modelResponse = response.firstText();
            if (!StringUtils.hasText(modelResponse)) {
                throw new IllegalStateException("Modelo não retornou conteúdo para gera-landing");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new ExperimentPipelineJobCompletionPayload(
                    modelResponse,
                    safeJson(response),
                    safeJson(payload),
                    response.id(),
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI no gera-landing [jobId={}, stage={}, status={}, responseBody={}]",
                    job.id(), job.section(), statusCode.value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar conteúdo de gera-landing", ex);
        }
    }

    private OpenAiResponse callOpenAi(Map<String, Object> payload) {
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        if (response == null) {
            throw new IllegalStateException("Resposta vazia da OpenAI");
        }
        if (response.hasError()) {
            throw new IllegalStateException(response.errorMessage());
        }
        return response;
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}

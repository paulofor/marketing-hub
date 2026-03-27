package com.marketinghub.worker.hypothesisframework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HypothesisFrameworkOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkOpenAiClient.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;

    public HypothesisFrameworkOpenAiClient(WebClient.Builder builder,
                                           ObjectMapper objectMapper,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de framework de hipótese ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public HypothesisFrameworkJobCompletionPayload generate(HypothesisFrameworkJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        Map<String, Object> payload = null;
        try {
            payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {
            });
            log.info("Enviando requisição para OpenAI [jobId={}, hypothesisId={}, section={}, model={}]",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    job.model());
            log.debug("Payload OpenAI [jobId={}]: {}", job.id(), safeJson(payload));
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
            String content = response.firstText();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("Modelo não retornou conteúdo");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new HypothesisFrameworkJobCompletionPayload(
                    content,
                    content,
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP ao chamar OpenAI [jobId={}, hypothesisId={}, section={}, model={}, status={}, responseBody={}]",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    job.model(),
                    statusCode.value(),
                    truncate(ex.getResponseBodyAsString()));
            log.debug("Payload da requisição com erro [jobId={}]: {}", job.id(), safeJson(payload));
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        }
    }

    private String safeJson(Object value) {
        if (value == null) {
            return "<null>";
        }
        try {
            return truncate(objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            return "<erro ao serializar payload: " + ex.getMessage() + ">";
        }
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "<vazio>";
        }
        int maxLength = 3_000;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }
}

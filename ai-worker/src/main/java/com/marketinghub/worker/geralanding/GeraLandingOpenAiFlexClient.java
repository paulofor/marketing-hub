package com.marketinghub.worker.geralanding;

import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
/**
 * Cliente de integração com a OpenAI para execução dos jobs do GeraLanding em modo flex.
 */
public class GeraLandingOpenAiFlexClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingOpenAiFlexClient.class);
    private static final int LOG_PREVIEW_LIMIT = 1200;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final Duration flexTimeout;

    public GeraLandingOpenAiFlexClient(WebClient.Builder builder,
                                        ObjectMapper objectMapper,
                                        @Value("${openai.api-key:}") String apiKey,
                                        @Value("${openai.base-url:https://api.request.com/v1}") String baseUrl,
                                        @Value("${openai.flex-timeout:${openai.batch-timeout:PT30M}}") Duration flexTimeout) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.flexTimeout = flexTimeout != null && !flexTimeout.isNegative() && !flexTimeout.isZero()
                ? flexTimeout
                : Duration.ofMinutes(30);

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


    /**
     * Executa a geração no endpoint /responses da OpenAI para DTO da etapa deliverables.
     */
    public GeraLandingJobCompletionPayload generate(com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingJobDto job) {
        return generateFromValues(job.id(), job.section(), job.model(), job.requestBodyJson(), job.prompt());
    }

    /**
     * Executa a geração no endpoint /responses da OpenAI e devolve o payload final da etapa.
     */
    public GeraLandingJobCompletionPayload generate(GeraLandingJobDto job) {
        return generateFromValues(job.id(), job.section(), job.model(), job.requestBodyJson(), job.prompt());
    }

    /**
     * Executa a geração no endpoint /responses da OpenAI com os valores normalizados do job.
     */
    private GeraLandingJobCompletionPayload generateFromValues(java.util.UUID jobId, String section, String model, String requestBodyJson, String prompt) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        try {
            log.info("Iniciando geração gera-landing via OpenAI flex [jobId={}, stage={}, model={}, requestBodyLength={}]",
                    jobId, section, model, safeLength(requestBodyJson));
            log.info("Payload requestBodyJson do gera-landing [jobId={}, stage={}, requestBodyPreview={}]",
                    jobId, section, preview(requestBodyJson));
            OpenAiResponse response = createFlexResponse(jobId, section, model, requestBodyJson, prompt);
            String rawOutput = objectMapper.writeValueAsString(response);
            String modelResponse = sanitizeModelResponse(response.firstText(), jobId, section);
            log.info("Resposta OpenAI recebida para gera-landing [jobId={}, stage={}, responseId={}, rawOutputLength={}, modelResponseLength={}, modelResponsePreview={}]",
                    jobId, section, response.id(), safeLength(rawOutput), safeLength(modelResponse), preview(modelResponse));
            if (!StringUtils.hasText(modelResponse)) {
                throw new IllegalStateException("Modelo não retornou conteúdo para gera-landing");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            log.info("Finalizando geração gera-landing [jobId={}, stage={}, responseId={}, inputTokens={}, outputTokens={}]",
                    jobId, section, response.id(), inputTokens, outputTokens);
            return new GeraLandingJobCompletionPayload(modelResponse, rawOutput, requestBodyJson, response.id(), inputTokens, outputTokens, OpenAiCostEstimator.estimateUsd(model, response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI flex no gera-landing [jobId={}, stage={}, status={}, responseBody={}]",
                    jobId, section, statusCode.value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing em modo flex", ex);
        } catch (Exception ex) {
            log.error("Falha inesperada no gera-landing em modo flex [jobId={}, stage={}, model={}, requestBodyPreview={}]",
                    jobId, section, model, preview(requestBodyJson), ex);
            throw new IllegalStateException("Falha ao gerar conteúdo de gera-landing em modo flex", ex);
        }
    }

    private OpenAiResponse createFlexResponse(java.util.UUID jobId, String section, String model, String requestBodyJson, String prompt) throws Exception {
        log.info("Iniciando preparação do request para flex [jobId={}, stage={}, requestBodyJsonPreview={}]",
                jobId, section, preview(requestBodyJson));
        Map<String, Object> requestBody = prepareRequestBodyForFlex(jobId, section, model, requestBodyJson, prompt);
        log.info("RequestBody parseado para flex [jobId={}, stage={}, keys={}, requestBodyMapPreview={}]",
                jobId, section, requestBody.keySet(), preview(objectMapper.writeValueAsString(requestBody)));
        requestBody.put("service_tier", "flex");
        log.info("RequestBody final para /responses [jobId={}, stage={}, requestBodyWithFlexPreview={}]",
                jobId, section, preview(objectMapper.writeValueAsString(requestBody)));

        OpenAiResponse response = webClient.post().uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block(flexTimeout);

        if (response == null) {
            throw new IllegalStateException("OpenAI flex retornou resposta vazia para gera-landing");
        }
        if (StringUtils.hasText(response.errorMessage())) {
            throw new IllegalStateException("OpenAI flex retornou erro para gera-landing: " + response.errorMessage());
        }
        return response;
    }

    Map<String, Object> prepareRequestBodyForFlex(java.util.UUID jobId, String section, String model, String requestBodyJson, String prompt) throws Exception {
        
        if (StringUtils.hasText(requestBodyJson)) {
            String trimmed = requestBodyJson.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<>() {});
            }
            log.warn("requestBodyJson não está em JSON; aplicando fallback para payload estruturado [jobId={}, stage={}, startsWith={}]",
                    jobId, section, trimmed.substring(0, Math.min(40, trimmed.length())).replace("\n", "\\n"));
            return buildRequestBodyFromPrompt(jobId, section, model, trimmed);
        }
        return buildRequestBodyFromPrompt(jobId, section, model, prompt);
    }


    /**
     * Remove cercas markdown de JSON (```json ... ```) para manter o conteúdo parseável no pipeline.
     */
    String sanitizeModelResponse(String modelResponse, java.util.UUID jobId, String section) {
        if (!StringUtils.hasText(modelResponse)) {
            return modelResponse;
        }
        String trimmed = modelResponse.trim();
        if (!trimmed.startsWith("```json")) {
            return modelResponse;
        }

        String withoutPrefix = trimmed.substring("```json".length()).trim();
        if (withoutPrefix.endsWith("```")) {
            withoutPrefix = withoutPrefix.substring(0, withoutPrefix.length() - 3).trim();
        }
        log.warn("Model response veio com code fence JSON; removendo cercas markdown [jobId={}, stage={}]",
                jobId, section);
        return withoutPrefix;
    }

    /**
     * Cria um corpo mínimo de requisição quando apenas o prompt textual está disponível.
     */
    private Map<String, Object> buildRequestBodyFromPrompt(java.util.UUID jobId, String section, String model, String promptText) {
        if (!StringUtils.hasText(promptText)) {
            throw new IllegalStateException("Payload da OpenAI ausente: requestBodyJson e prompt vazios para gera-landing");
        }
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(model) ? model : "gpt-5.2");
        requestBody.put("input", List.of(
                Map.of("role", "system", "content", "[gera-landing-pipeline] Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet."),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", promptText)))));
        return requestBody;
    }

    private int safeLength(String value) {
        return value != null ? value.length() : 0;
    }

    private String preview(String value) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        String normalized = value.replace("\n", "\\n").replace("\r", "\\r");
        if (normalized.length() <= LOG_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, LOG_PREVIEW_LIMIT) + "...(truncated)";
    }
}

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
     * Executa a geração no endpoint /responses da OpenAI e devolve o payload final da etapa.
     */
    public GeraLandingJobCompletionPayload generate(GeraLandingJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        try {
            log.info("Iniciando geração gera-landing via OpenAI flex [jobId={}, stage={}, model={}, requestBodyLength={}]",
                    job.id(), job.section(), job.model(), safeLength(job.requestBodyJson()));
            log.info("Payload requestBodyJson do gera-landing [jobId={}, stage={}, requestBodyPreview={}]",
                    job.id(), job.section(), preview(job.requestBodyJson()));
            OpenAiResponse response = createFlexResponse(job);
            String rawOutput = objectMapper.writeValueAsString(response);
            String modelResponse = sanitizeModelResponse(response.firstText(), job);
            log.info("Resposta OpenAI recebida para gera-landing [jobId={}, stage={}, responseId={}, rawOutputLength={}, modelResponseLength={}, modelResponsePreview={}]",
                    job.id(),
                    job.section(),
                    response.id(),
                    safeLength(rawOutput),
                    safeLength(modelResponse),
                    preview(modelResponse));
            if (!StringUtils.hasText(modelResponse)) {
                throw new IllegalStateException("Modelo não retornou conteúdo para gera-landing");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            log.info("Finalizando geração gera-landing [jobId={}, stage={}, responseId={}, inputTokens={}, outputTokens={}]",
                    job.id(), job.section(), response.id(), inputTokens, outputTokens);
            return new GeraLandingJobCompletionPayload(
                    modelResponse,
                    rawOutput,
                    job.requestBodyJson(),
                    response.id(),
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI flex no gera-landing [jobId={}, stage={}, status={}, responseBody={}]",
                    job.id(), job.section(), statusCode.value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing em modo flex", ex);
        } catch (Exception ex) {
            log.error("Falha inesperada no gera-landing em modo flex [jobId={}, stage={}, model={}, requestBodyPreview={}]",
                    job.id(), job.section(), job.model(), preview(job.requestBodyJson()), ex);
            throw new IllegalStateException("Falha ao gerar conteúdo de gera-landing em modo flex", ex);
        }
    }

    private OpenAiResponse createFlexResponse(GeraLandingJobDto job) throws Exception {
        log.info("Iniciando preparação do request para flex [jobId={}, stage={}, requestBodyJsonPreview={}]",
                job.id(), job.section(), preview(job.requestBodyJson()));
        Map<String, Object> requestBody = prepareRequestBodyForFlex(job);
        log.info("RequestBody parseado para flex [jobId={}, stage={}, keys={}, requestBodyMapPreview={}]",
                job.id(), job.section(), requestBody.keySet(), preview(objectMapper.writeValueAsString(requestBody)));
        requestBody.put("service_tier", "flex");
        log.info("RequestBody final para /responses [jobId={}, stage={}, requestBodyWithFlexPreview={}]",
                job.id(), job.section(), preview(objectMapper.writeValueAsString(requestBody)));

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

    Map<String, Object> prepareRequestBodyForFlex(GeraLandingJobDto job) throws Exception {
        String requestBodyJson = job.requestBodyJson();
        if (StringUtils.hasText(requestBodyJson)) {
            String trimmed = requestBodyJson.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<>() {});
            }
            log.warn("requestBodyJson não está em JSON; aplicando fallback para payload estruturado [jobId={}, stage={}, startsWith={}]",
                    job.id(), job.section(), trimmed.substring(0, Math.min(40, trimmed.length())).replace("\n", "\\n"));
            return buildRequestBodyFromPrompt(job, trimmed);
        }
        return buildRequestBodyFromPrompt(job, job.prompt());
    }


    /**
     * Remove cercas markdown de JSON (```json ... ```) para manter o conteúdo parseável no pipeline.
     */
    String sanitizeModelResponse(String modelResponse, GeraLandingJobDto job) {
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
                job.id(), job.section());
        return withoutPrefix;
    }

    /**
     * Cria um corpo mínimo de requisição quando apenas o prompt textual está disponível.
     */
    private Map<String, Object> buildRequestBodyFromPrompt(GeraLandingJobDto job, String promptText) {
        if (!StringUtils.hasText(promptText)) {
            throw new IllegalStateException("Payload da OpenAI ausente: requestBodyJson e prompt vazios para gera-landing");
        }
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(job.model()) ? job.model() : "gpt-5.2");
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

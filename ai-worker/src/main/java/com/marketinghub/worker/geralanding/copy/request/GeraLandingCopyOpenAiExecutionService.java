package com.marketinghub.worker.geralanding.copy.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.copy.backend.GeraLandingCopyBackendClient;
import com.marketinghub.worker.geralanding.copy.GeraLandingExperimentRequest;
import com.marketinghub.worker.geralanding.copy.GeraLandingJobCompletionPayload;
import com.marketinghub.worker.geralanding.copy.response.RecebeResponse;
import com.marketinghub.worker.geralanding.copy.dto.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Responsabilidade: executar jobs OpenAI da etapa copy de forma isolada no pacote da etapa. */
@Service
public class GeraLandingCopyOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingCopyOpenAiExecutionService.class);
    private static final int LOG_PREVIEW_LIMIT = 1200;
    private final GeraLandingCopyBackendClient backendClient;
    private final com.marketinghub.worker.geralanding.copy.request.MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final Duration flexTimeout;

    public GeraLandingCopyOpenAiExecutionService(GeraLandingCopyBackendClient backendClient, WebClient.Builder builder, ObjectMapper objectMapper,
                                                 @Value("${openai.api-key:}") String apiKey,
                                                 @Value("${openai.base-url:https://api.request.com/v1}") String baseUrl,
                                                 @Value("${openai.flex-timeout:${openai.batch-timeout:PT30M}}") Duration flexTimeout,
                                                 com.marketinghub.worker.geralanding.copy.request.MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient;
        this.montaRequest = montaRequest;
        this.recebeResponse = recebeResponse;
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.flexTimeout = flexTimeout != null && !flexTimeout.isNegative() && !flexTimeout.isZero() ? flexTimeout : Duration.ofMinutes(30);
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        else log.warn("OPENAI_API_KEY não configurada; jobs de gera-landing ficarão pendentes");
        this.webClient = clientBuilder.build();
    }

    /** Processa os jobs pendentes da etapa copy. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        if (!enabled) return;
        for (GeraLandingStageExecutionDetailDto execution : jobs) processExecution(execution);
    }

    /** Processa um job individual da etapa copy. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentRequest requestData = new GeraLandingExperimentRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData);
            String requestBody = montaRequest.montar(requestData);
            String openAiModel = "gpt-5.2";
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), openAiModel, requestBody, prompt, null);
            var payload = generate(openAiJob);
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa copy para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }

    /** Executa a geração no endpoint /responses da OpenAI e devolve o payload final da etapa. */
    private GeraLandingJobCompletionPayload generate(GeraLandingJobDto job) { /* shortened by parity */
        try {
            OpenAiResponse response = createFlexResponse(job);
            String rawOutput = objectMapper.writeValueAsString(response);
            String modelResponse = sanitizeModelResponse(response.firstText(), job);
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new GeraLandingJobCompletionPayload(modelResponse, rawOutput, job.requestBodyJson(), response.id(), inputTokens, outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI flex no gera-landing [jobId={}, stage={}, status={}, responseBody={}]", job.id(), job.section(), statusCode.value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing em modo flex", ex);
        } catch (Exception ex) {
            log.error("Falha inesperada no gera-landing em modo flex [jobId={}, stage={}, model={}, requestBodyPreview={}]", job.id(), job.section(), job.model(), preview(job.requestBodyJson()), ex);
            throw new IllegalStateException("Falha ao gerar conteúdo de gera-landing em modo flex", ex);
        }
    }

    private OpenAiResponse createFlexResponse(GeraLandingJobDto job) throws Exception { Map<String, Object> requestBody = prepareRequestBodyForFlex(job); requestBody.put("service_tier", "flex"); OpenAiResponse response = webClient.post().uri("/responses").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody).retrieve().bodyToMono(OpenAiResponse.class).block(flexTimeout); if (response == null) throw new IllegalStateException("OpenAI flex retornou resposta vazia para gera-landing"); if (StringUtils.hasText(response.errorMessage())) throw new IllegalStateException("OpenAI flex retornou erro para gera-landing: " + response.errorMessage()); return response; }
    private Map<String, Object> prepareRequestBodyForFlex(GeraLandingJobDto job) throws Exception { String requestBodyJson = job.requestBodyJson(); if (StringUtils.hasText(requestBodyJson)) { String trimmed = requestBodyJson.trim(); if (trimmed.startsWith("{") || trimmed.startsWith("[")) return objectMapper.readValue(trimmed, new TypeReference<>() {}); return buildRequestBodyFromPrompt(job, trimmed);} return buildRequestBodyFromPrompt(job, job.prompt()); }
    private String sanitizeModelResponse(String modelResponse, GeraLandingJobDto job) { if (!StringUtils.hasText(modelResponse)) return modelResponse; String trimmed = modelResponse.trim(); if (!trimmed.startsWith("```json")) return modelResponse; String withoutPrefix = trimmed.substring("```json".length()).trim(); if (withoutPrefix.endsWith("```")) withoutPrefix = withoutPrefix.substring(0, withoutPrefix.length() - 3).trim(); log.warn("Model response veio com code fence JSON; removendo cercas markdown [jobId={}, stage={}]", job.id(), job.section()); return withoutPrefix; }
    private Map<String, Object> buildRequestBodyFromPrompt(GeraLandingJobDto job, String promptText) { if (!StringUtils.hasText(promptText)) throw new IllegalStateException("Payload da OpenAI ausente: requestBodyJson e prompt vazios para gera-landing"); Map<String, Object> requestBody = new LinkedHashMap<>(); requestBody.put("model", StringUtils.hasText(job.model()) ? job.model() : "gpt-5.2"); requestBody.put("input", List.of(Map.of("role", "system", "content", "[gera-landing-pipeline] Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet."), Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", promptText))))); return requestBody; }
    private String preview(String value) { if (!StringUtils.hasText(value)) return "<empty>"; String normalized = value.replace("\n", "\\n").replace("\r", "\\r"); if (normalized.length() <= LOG_PREVIEW_LIMIT) return normalized; return normalized.substring(0, LOG_PREVIEW_LIMIT) + "...(truncated)"; }
}

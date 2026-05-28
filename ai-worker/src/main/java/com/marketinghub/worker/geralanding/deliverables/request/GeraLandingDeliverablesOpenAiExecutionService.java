package com.marketinghub.worker.geralanding.deliverables.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingDeliverablesBackendClient;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingExperimentDeliverablesRequest;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingJobCompletionDeliverablesPayload;
import com.marketinghub.worker.geralanding.deliverables.MontaRequest;
import com.marketinghub.worker.geralanding.deliverables.RecebeResponse;
import com.marketinghub.worker.geralanding.deliverables.dto.RecordJobDto;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
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

/** Responsabilidade: executar jobs OpenAI da etapa deliverables de forma isolada no pacote da etapa. */
@Service
public class GeraLandingDeliverablesOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingDeliverablesOpenAiExecutionService.class);
    private static final int LOG_PREVIEW_LIMIT = 1200;

    private final GeraLandingDeliverablesBackendClient backendClient;
    private final MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final Duration flexTimeout;

    public GeraLandingDeliverablesOpenAiExecutionService(
            GeraLandingDeliverablesBackendClient backendClient,
            MontaRequest montaRequest,
            RecebeResponse recebeResponse,
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.request.com/v1}") String baseUrl,
            @Value("${openai.flex-timeout:${openai.batch-timeout:PT30M}}") Duration flexTimeout) {
        this.backendClient = backendClient;
        this.montaRequest = montaRequest;
        this.recebeResponse = recebeResponse;
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.flexTimeout = flexTimeout != null && !flexTimeout.isNegative() && !flexTimeout.isZero() ? flexTimeout : Duration.ofMinutes(30);

        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de gera-landing deliverables ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    /** Processa os jobs pendentes da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        if (!enabled) {
            return;
        }
        for (GeraLandingStageExecutionDetailDto execution : jobs) {
            processExecution(execution);
        }
    }

    /** Processa um job individual da etapa deliverables. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) {
            return;
        }
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentDeliverablesRequest requestData =
                    new GeraLandingExperimentDeliverablesRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData);
            String requestBody = montaRequest.montar(requestData);
            RecordJobDto openAiJob = new RecordJobDto(
                    UUID.fromString(execution.idJob()),
                    execution.experimentId(),
                    execution.stageCode(),
                    "gpt-5.2",
                    requestBody,
                    prompt,
                    null);
            GeraLandingJobCompletionDeliverablesPayload payload = generate(openAiJob);
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa deliverables para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }

    /** Executa a geração no endpoint /responses da OpenAI e devolve o payload final da etapa deliverables. */
    private GeraLandingJobCompletionDeliverablesPayload generate(RecordJobDto job) throws Exception {
        log.info("Iniciando geração gera-landing deliverables via OpenAI flex [jobId={}, stage={}, model={}, requestBodyLength={}]", job.id(), job.section(), job.model(), safeLength(job.requestBodyJson()));
        log.info("Payload requestBodyJson do gera-landing deliverables [jobId={}, stage={}, requestBodyPreview={}]", job.id(), job.section(), preview(job.requestBodyJson()));
        OpenAiResponse response = createFlexResponse(job);
        String rawOutput = objectMapper.writeValueAsString(response);
        String modelResponse = sanitizeModelResponse(response.firstText(), job);

        log.info("Resposta OpenAI recebida para gera-landing deliverables [jobId={}, stage={}, responseId={}, rawOutputLength={}, modelResponseLength={}, modelResponsePreview={}]", job.id(), job.section(), response.id(), safeLength(rawOutput), safeLength(modelResponse), preview(modelResponse));
        if (!StringUtils.hasText(modelResponse)) {
            throw new IllegalStateException("Modelo não retornou conteúdo para gera-landing deliverables");
        }

        Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
        Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
        log.info("Finalizando geração gera-landing deliverables [jobId={}, stage={}, responseId={}, inputTokens={}, outputTokens={}]", job.id(), job.section(), response.id(), inputTokens, outputTokens);
        return new GeraLandingJobCompletionDeliverablesPayload(
                modelResponse,
                rawOutput,
                job.requestBodyJson(),
                response.id(),
                inputTokens,
                outputTokens,
                OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
    }

    /** Prepara e executa a chamada flex da OpenAI para a etapa deliverables. */
    private OpenAiResponse createFlexResponse(RecordJobDto job) throws Exception {
        try {
            log.info("Iniciando preparação do request para flex deliverables [jobId={}, stage={}, requestBodyJsonPreview={}]", job.id(), job.section(), preview(job.requestBodyJson()));
            Map<String, Object> requestBody = prepareRequestBodyForFlex(job);
            log.info("RequestBody parseado para flex deliverables [jobId={}, stage={}, keys={}, requestBodyMapPreview={}]", job.id(), job.section(), requestBody.keySet(), preview(objectMapper.writeValueAsString(requestBody)));
            requestBody.put("service_tier", "flex");
            log.info("RequestBody final para /responses deliverables [jobId={}, stage={}, requestBodyWithFlexPreview={}]", job.id(), job.section(), preview(objectMapper.writeValueAsString(requestBody)));

            OpenAiResponse response = webClient.post().uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(flexTimeout);

            if (response == null) {
                throw new IllegalStateException("OpenAI flex retornou resposta vazia para gera-landing deliverables");
            }
            if (StringUtils.hasText(response.errorMessage())) {
                throw new IllegalStateException("OpenAI flex retornou erro para gera-landing deliverables: " + response.errorMessage());
            }
            return response;
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI flex no gera-landing deliverables [jobId={}, stage={}, status={}, responseBody={}]", job.id(), job.section(), statusCode.value(), ex.getResponseBodyAsString(), ex);
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing deliverables em modo flex", ex);
        } catch (Exception ex) {
            log.error("Falha inesperada no gera-landing deliverables em modo flex [jobId={}, stage={}, model={}, requestBodyPreview={}]", job.id(), job.section(), job.model(), preview(job.requestBodyJson()), ex);
            throw ex;
        }
    }

    /** Converte o requestBodyJson em mapa ou aplica fallback usando prompt textual. */
    private Map<String, Object> prepareRequestBodyForFlex(RecordJobDto job) throws Exception {
        String requestBodyJson = job.requestBodyJson();
        if (StringUtils.hasText(requestBodyJson)) {
            String trimmed = requestBodyJson.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<>() {});
            }
            log.warn("requestBodyJson deliverables não está em JSON; aplicando fallback [jobId={}, stage={}, startsWith={}]", job.id(), job.section(), trimmed.substring(0, Math.min(40, trimmed.length())).replace("\n", "\\n"));
            return buildRequestBodyFromPrompt(job, trimmed);
        }
        return buildRequestBodyFromPrompt(job, job.prompt());
    }

    /** Remove cercas markdown de JSON (```json ... ```) para manter conteúdo parseável no pipeline. */
    private String sanitizeModelResponse(String modelResponse, RecordJobDto job) {
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
        log.warn("Model response deliverables veio com code fence JSON; removendo cercas markdown [jobId={}, stage={}]", job.id(), job.section());
        return withoutPrefix;
    }

    /** Cria um corpo mínimo de requisição quando apenas o prompt textual está disponível. */
    private Map<String, Object> buildRequestBodyFromPrompt(RecordJobDto job, String promptText) {
        if (!StringUtils.hasText(promptText)) {
            throw new IllegalStateException("Payload da OpenAI ausente: requestBodyJson e prompt vazios para gera-landing deliverables");
        }
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(job.model()) ? job.model() : "gpt-5.2");
        requestBody.put("input", List.of(
                Map.of("role", "system", "content", "[gera-landing-pipeline] Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet."),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", promptText)))));
        return requestBody;
    }

    /** Retorna tamanho seguro de strings para log. */
    private int safeLength(String value) {
        return value != null ? value.length() : 0;
    }

    /** Retorna prévia compactada e truncada para logs. */
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

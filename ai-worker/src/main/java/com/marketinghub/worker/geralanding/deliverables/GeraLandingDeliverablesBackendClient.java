package com.marketinghub.worker.geralanding.deliverables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Encapsula o acesso HTTP ao backend para operações específicas da etapa deliverables. */
@Component
public class GeraLandingDeliverablesBackendClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingDeliverablesBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper;

    public GeraLandingDeliverablesBackendClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${backend.api-prefix:/api}") String apiPrefix,
            ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.objectMapper = objectMapper;
    }

    /** Lista execuções pendentes da etapa deliverables no endpoint interno específico da etapa. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/deliverables/stage-executions/pending")
                + "?limit=" + Math.max(1, limit);
        List<GeraLandingStageExecutionDetailDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .doOnError(err -> log.error("Falha ao buscar execuções pendentes deliverables. uri={}", uri, err))
                .block();
        return payload != null ? payload : List.of();
    }

    /** Envia resultado da etapa deliverables ao endpoint interno específico da etapa. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionDeliverablesPayload payload) {
        String baseUrl = stageExecutionBaseUrl();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("modelResponse", payload != null ? payload.responseContent() : null);
        body.put("inputTokens", payload != null ? payload.inputTokens() : null);
        body.put("outputTokens", payload != null ? payload.outputTokens() : null);
        body.put("costUsd", payload != null ? payload.costUsd() : null);
        body.put("openAiJobId", payload != null ? payload.openAiJobId() : null);
        webClient.post().uri(baseUrl + "/{idJob}/receive-result", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Encaminha confirmação de despacho da etapa deliverables ao backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        String baseUrl = stageExecutionBaseUrl();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("openAiJobId", openAiJobId);
        webClient.post().uri(baseUrl + "/{idJob}/receive-dispatch", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Carrega os dados de prompt necessários para execução da etapa deliverables. */
    public Map<String, Object> loadPromptData(Long experimentId) {
        if (experimentId == null) {
            return Map.of();
        }
        String experimentUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId);
        Map<String, Object> experiment = webClient.get().uri(experimentUrl).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
        if (experiment == null || experiment.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("singlePain", experiment.get("singlePain"));
        payload.put("freeReward", experiment.get("freeReward"));
        payload.put("funnelPromise", experiment.get("funnelPromise"));
        payload.put("primaryCta", experiment.get("primaryCta"));
        payload.put("campaignObjective", experiment.get("campaignObjective"));
        payload.put("campaignAngle", parseJsonField(experiment.get("campaignAngle")));
        payload.put("adCopy", parseJsonField(experiment.get("adCopy")));
        payload.put("adImageBriefing", parseJsonField(experiment.get("adImageBriefing")));
        payload.put("landingPageWireframe", parseJsonField(experiment.get("landingPageWireframe")));
        payload.put("NICHE_NAME", resolveNicheName(experiment.get("nicheId")));
        populateHypothesisFields(payload, experiment.get("nicheId"), experiment.get("hypothesisId"));
        return payload;
    }

    /** Encaminha falha de execução da etapa deliverables ao endpoint interno específico da etapa. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        String baseUrl = stageExecutionBaseUrl();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post().uri(baseUrl + "/{idJob}/receive-result", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Busca os detalhes de execução da etapa deliverables no controller público da etapa. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        String uri = UrlUtils.joinPath(
                backendBaseUrl,
                apiPrefix,
                "/experiments/" + experimentId + "/geralanding/deliverables/stage-executions/" + idJob);
        return webClient.get().uri(uri).retrieve().bodyToMono(GeraLandingStageExecutionDetailDto.class).onErrorReturn(null).block();
    }

    /** Monta a URL base interna da etapa deliverables. */
    private String stageExecutionBaseUrl() {
        return UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/deliverables/stage-executions");
    }

    /** Preenche dados de hipótese no payload de prompt quando disponíveis. */
    private void populateHypothesisFields(Map<String, Object> payload, Object nicheId, Object hypothesisId) {
        if (nicheId == null || hypothesisId == null) {
            return;
        }
        String hypothesesUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId + "/hypotheses");
        List<Map<String, Object>> hypotheses = webClient.get()
                .uri(hypothesesUrl)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .onErrorReturn(List.of())
                .block();
        if (hypotheses == null || hypotheses.isEmpty()) {
            return;
        }
        String hypothesisIdText = String.valueOf(hypothesisId);
        hypotheses.stream()
                .filter(item -> hypothesisIdText.equalsIgnoreCase(String.valueOf(item.get("id"))))
                .findFirst()
                .ifPresent(hypothesis -> {
                    Object framework = hypothesis.get("framework");
                    if (framework instanceof Map<?, ?> map) {
                        Object pain = map.get("pain");
                        Object result = map.get("result");
                        if (pain != null) {
                            payload.put("PAIN_JSON", pain);
                        }
                        if (result != null) {
                            payload.put("RESULT_JSON", result);
                        }
                    }
                });
    }

    /** Resolve o nome do nicho usando o endpoint de nichos do backend. */
    private String resolveNicheName(Object nicheId) {
        if (nicheId == null) {
            return "";
        }
        String nicheUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId);
        Map<String, Object> niche = webClient.get().uri(nicheUrl).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
        return niche != null && niche.get("name") != null ? String.valueOf(niche.get("name")) : "";
    }

    /** Tenta converter campo JSON serializado para mapa mantendo valor cru quando inválido. */
    private Object parseJsonField(Object value) {
        if (!(value instanceof String raw) || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            return raw;
        }
    }

    /** Trata resposta de listagem pendente e propaga erros HTTP com contexto operacional. */
    private Flux<GeraLandingStageExecutionDetailDto> handleListResponse(String uri, HttpStatusCode status, ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(GeraLandingStageExecutionDetailDto.class);
    }
}

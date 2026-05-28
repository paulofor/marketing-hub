package com.marketinghub.worker.geralanding.wireframe.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.wireframe.response.GeraLandingJobCompletionWireframePayload;
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

/** Responsabilidade: encapsular as integrações HTTP da etapa wireframe com o backend sem dependências cruzadas de outras etapas. */
@Component
public class GeraLandingWireframeBackendClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingWireframeBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper;

    public GeraLandingWireframeBackendClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${backend.api-prefix:/api}") String apiPrefix,
            ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.objectMapper = objectMapper;
    }

    /** Lista execuções pendentes da etapa wireframe no backend. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        List<GeraLandingStageExecutionDetailDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .doOnError(err -> log.error("Falha ao buscar execuções pendentes wireframe. uri={}", uri, err))
                .block();
        return payload != null ? payload : List.of();
    }

    /** Carrega os dados de prompt para a geração wireframe a partir do experimento. */
    public Map<String, Object> loadPromptData(Long experimentId) {
        if (experimentId == null) {
            return Map.of();
        }
        String experimentUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId);
        Map<String, Object> experiment = webClient.get()
                .uri(experimentUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .block();
        if (experiment == null || experiment.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignAngle", parseJsonField(experiment.get("campaignAngle")));
        payload.put("adCopy", parseJsonField(experiment.get("adCopy")));
        payload.put("adImageBriefing", parseJsonField(experiment.get("adImageBriefing")));
        payload.put("landingPageWireframe", parseJsonField(experiment.get("landingPageWireframe")));
        payload.put("NICHE_NAME", resolveNicheName(experiment.get("nicheId")));
        populateHypothesisFields(payload, experiment.get("nicheId"), experiment.get("hypothesisId"));
        return payload;
    }

    /** Envia estado de falha da execução wireframe ao backend. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post().uri(baseUrl + "/{idJob}/receive-result", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Registra no backend o dispatch do job wireframe para a OpenAI. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("openAiJobId", openAiJobId);
        webClient.post().uri(baseUrl + "/{idJob}/receive-dispatch", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Envia ao backend o resultado final da etapa wireframe. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionWireframePayload payload) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions");
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

    /** Busca os detalhes de uma execução específica da etapa wireframe. */
    public GeraLandingStageExecutionDetailDto fetchWireframeStageExecutionDetail(Long experimentId, String idJob) {
        String uri = UrlUtils.joinPath(
                backendBaseUrl,
                apiPrefix,
                "/experiments/" + experimentId + "/geralanding/wireframe/stage-executions/" + idJob);
        return webClient.get().uri(uri).retrieve().bodyToMono(GeraLandingStageExecutionDetailDto.class).onErrorReturn(null).block();
    }

    /** Preenche no payload dados de hipótese usados para montar o prompt da etapa. */
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

    /** Tenta converter campos JSON serializados para mapa, mantendo texto cru quando inválido. */
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

    /** Trata resposta de listagem pendente e converte erros HTTP em exceção contextual. */
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

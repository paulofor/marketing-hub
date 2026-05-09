package com.marketinghub.worker.geralanding;

import com.marketinghub.worker.util.UrlUtils;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GeraLandingBackendClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public GeraLandingBackendClient(WebClient.Builder builder,
                                    @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                    @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<GeraLandingStageExecutionDto> listPendingExecutions(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        log.info("Fetching pending gera-landing stage executions from {}", uri);
        List<GeraLandingStageExecutionDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .doOnError(err -> log.error("Failed to fetch pending gera-landing stage executions from {}", uri, err))
                .block();
        List<GeraLandingStageExecutionDto> result = payload != null ? payload : List.of();
        log.info("Backend returned {} pending gera-landing stage execution(s)", result.size());
        return result;
    }

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
        payload.put("NICHE_NAME", resolveNicheName(experiment.get("nicheId")));
        populateHypothesisFields(payload, experiment.get("nicheId"), experiment.get("hypothesisId"));
        return payload;
    }

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

    private String resolveNicheName(Object nicheId) {
        if (nicheId == null) {
            return "";
        }
        String nicheUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId);
        Map<String, Object> niche = webClient.get()
                .uri(nicheUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .block();
        return niche != null && niche.get("name") != null ? String.valueOf(niche.get("name")) : "";
    }

    private Object parseJsonField(Object value) {
        if (!(value instanceof String raw) || raw.isBlank()) {
            return Map.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, Map.class);
        } catch (Exception ex) {
            return raw;
        }
    }

    public void receivePrompt(String idJob,
                              Long experimentId,
                              String stageCode,
                              String prompt,
                              String openAiRequestBody,
                              String openAiModel,
                              String schemaJson,
                              String promptMarkdownContent) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/stage-executions");
        log.info(
                "Sending gera-landing prompt. endpoint={}/{{idJob}}/receive-prompt, idJob={}, experimentId={}, stageCode={}, promptLength={}",
                baseUrl,
                idJob,
                experimentId,
                stageCode,
                prompt != null ? prompt.length() : 0);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("prompt", prompt);
        body.put("openAiRequestBody", openAiRequestBody);
        body.put("openAiModel", openAiModel);
        body.put("schemaJson", schemaJson);
        body.put("promptMarkdownContent", promptMarkdownContent);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-prompt", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }



    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionPayload payload) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("modelResponse", payload != null ? payload.responseContent() : null);
        body.put("inputTokens", payload != null ? payload.inputTokens() : null);
        body.put("outputTokens", payload != null ? payload.outputTokens() : null);
        body.put("costUsd", payload != null ? payload.costUsd() : null);
        body.put("openAiJobId", payload != null ? payload.openAiJobId() : null);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-result", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-result", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("openAiJobId", openAiJobId);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-dispatch", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private Flux<GeraLandingStageExecutionDto> handleListResponse(String uri,
                                                                  HttpStatusCode status,
                                                                  org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(GeraLandingStageExecutionDto.class);
    }
}

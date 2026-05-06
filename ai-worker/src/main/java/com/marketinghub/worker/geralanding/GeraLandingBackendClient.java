package com.marketinghub.worker.geralanding;

import com.marketinghub.worker.util.UrlUtils;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public void receivePrompt(String idJob, Long experimentId, String stageCode, String prompt) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/stage-executions");
        log.info(
                "Sending gera-landing prompt. endpoint={}/{{idJob}}/receive-prompt, idJob={}, experimentId={}, stageCode={}, promptLength={}",
                baseUrl,
                idJob,
                experimentId,
                stageCode,
                prompt != null ? prompt.length() : 0);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-prompt", idJob)
                .bodyValue(Map.of(
                        "experimentId", experimentId,
                        "stageCode", stageCode,
                        "prompt", prompt))
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
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-result", idJob)
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

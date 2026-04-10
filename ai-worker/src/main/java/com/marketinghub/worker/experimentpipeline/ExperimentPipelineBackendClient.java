package com.marketinghub.worker.experimentpipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.util.UrlUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ExperimentPipelineBackendClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExperimentPipelineBackendClient(WebClient.Builder builder,
                                           @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                           @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<ExperimentPipelineJobDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        List<ExperimentPipelineJobDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .onErrorResume(err -> {
                    log.error("Failed to fetch pending experiment pipeline jobs", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    public ExperimentPipelineJobDto claim(UUID jobId, String workerId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/claim");
        return webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("workerId", workerId))
                .retrieve()
                .bodyToMono(ExperimentPipelineJobDto.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void complete(UUID jobId, ExperimentPipelineJobCompletionPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/complete");
        log.info("POST /complete do pipeline (jobId={}, keys={})", jobId, summarizeCompletionPayloadKeys(payload));
        try {
            webClient.post()
                    .uri(url)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException ex) {
            String summarizedBody = summarizeErrorBody(ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 422) {
                log.error("Erro 422 ao completar job de pipeline {}: {}", jobId, summarizedBody);
            } else {
                log.error("Erro HTTP {} ao completar job de pipeline {}: {}",
                        ex.getStatusCode().value(), jobId, summarizedBody);
            }
            throw ex;
        }
    }

    public void fail(UUID jobId, String errorMessage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/fail");
        webClient.post()
                .uri(url)
                .bodyValue(new ExperimentPipelineJobFailurePayload(errorMessage))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void updateStage(UUID jobId, String stage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/stage");
        webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("stage", stage))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private Flux<ExperimentPipelineJobDto> handleListResponse(String uri,
                                                              HttpStatusCode status,
                                                              org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(ExperimentPipelineJobDto.class);
    }

    private String summarizeCompletionPayloadKeys(ExperimentPipelineJobCompletionPayload payload) {
        if (payload == null || payload.responseContent() == null) {
            return "[]";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(payload.responseContent(), Map.class);
            return parsed.keySet().toString();
        } catch (Exception ignored) {
            return "[unparseable-response-content]";
        }
    }

    private String summarizeErrorBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "[sem corpo de erro]";
        }
        String compact = rawBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }
}

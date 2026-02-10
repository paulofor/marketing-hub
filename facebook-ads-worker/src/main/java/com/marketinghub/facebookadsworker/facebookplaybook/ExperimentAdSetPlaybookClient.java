package com.marketinghub.facebookadsworker.facebookplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the backend playbook endpoints consumed by the Facebook worker.
 */
@Component
public class ExperimentAdSetPlaybookClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAdSetPlaybookClient.class);

    private final WebClient webClient;
    private final String baseUrl;

    public ExperimentAdSetPlaybookClient(WebClient.Builder builder,
                                         @Value("${backend.base-url:http://127.0.0.1:8080}") String backendBaseUrl) {
        this.webClient = builder.build();
        this.baseUrl = backendBaseUrl.endsWith("/")
                ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
                : backendBaseUrl;
    }

    public List<PlaybookJob> claimJobs(String workerId, int limit) {
        String url = baseUrl + "/internal/adset-playbook/jobs/claim";
        Map<String, Object> payload = Map.of(
                "worker", "FACEBOOK",
                "limit", Math.max(1, Math.min(limit, 20)),
                "workerId", workerId
        );
        LOGGER.debug("Facebook worker solicitando jobs em {}", url);
        return webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(JobPayloadResponse.class)
                .map(resp -> new PlaybookJob(
                        resp.id(),
                        PlaybookJobType.valueOf(resp.type()),
                        resp.workflowId(),
                        resp.resourceId(),
                        resp.payload(),
                        resp.createdAt()
                ))
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public void completeJob(long jobId, JsonNode result, List<ApiCallPayload> apiCalls) {
        String url = baseUrl + "/internal/adset-playbook/jobs/" + jobId + "/complete";
        Map<String, Object> body = new HashMap<>();
        body.put("result", result);
        body.put("apiCalls", apiCalls == null ? List.of() : apiCalls);
        webClient.post()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> handleVoidResponse("complete", url, response.statusCode()))
                .block();
    }

    public void failJob(long jobId, String error, List<ApiCallPayload> apiCalls) {
        String url = baseUrl + "/internal/adset-playbook/jobs/" + jobId + "/fail";
        Map<String, Object> body = new HashMap<>();
        body.put("errorMessage", error);
        body.put("apiCalls", apiCalls == null ? List.of() : apiCalls);
        webClient.post()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> handleVoidResponse("fail", url, response.statusCode()))
                .block();
    }

    private Mono<Void> handleVoidResponse(String action, String url, HttpStatusCode status) {
        if (status.isError()) {
            return Mono.error(new IllegalStateException("Backend " + action + " " + url + " retornou " + status));
        }
        return Mono.empty();
    }

    public record ApiCallPayload(String provider,
                                 String endpoint,
                                 String httpMethod,
                                 Integer statusCode,
                                 JsonNode requestPayload,
                                 JsonNode responsePayload,
                                 String errorMessage,
                                 Instant requestedAt,
                                 Instant respondedAt) {
    }

    private record JobPayloadResponse(long id,
                                      String type,
                                      JsonNode payload,
                                      Long workflowId,
                                      Long resourceId,
                                      Instant createdAt) {
    }
}

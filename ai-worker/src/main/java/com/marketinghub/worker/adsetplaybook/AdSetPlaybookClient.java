package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.worker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP client for the backend playbook job endpoints.
 */
@Component
public class AdSetPlaybookClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdSetPlaybookClient.class);

    private final WebClient webClient;
    private final String baseUrl;

    public AdSetPlaybookClient(WebClient.Builder builder,
                               @Value("${backend.base-url:http://127.0.0.1:8080}") String backendBaseUrl) {
        this.webClient = builder.build();
        this.baseUrl = backendBaseUrl.endsWith("/")
                ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
                : backendBaseUrl;
    }

    public List<PlaybookJob> claimJobs(PlaybookWorker worker, int limit, String workerId) {
        String url = baseUrl + "/internal/adset-playbook/jobs/claim";
        Map<String, Object> payload = Map.of(
                "worker", worker.name(),
                "limit", Math.max(1, Math.min(limit, 20)),
                "workerId", workerId
        );
        LOGGER.debug("Claiming playbook jobs at {}", url);
        return webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(JobPayloadResponse.class)
                .map(this::toJob)
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public void completeJob(long jobId, JsonNode result) {
        String url = baseUrl + "/internal/adset-playbook/jobs/" + jobId + "/complete";
        webClient.post()
                .uri(url)
                .bodyValue(Map.of("result", result))
                .exchangeToMono(response -> handleVoidResponse("complete", url, response.statusCode()))
                .block();
    }

    public void failJob(long jobId, String error) {
        String url = baseUrl + "/internal/adset-playbook/jobs/" + jobId + "/fail";
        webClient.post()
                .uri(url)
                .bodyValue(Map.of("errorMessage", Objects.requireNonNullElse(error, "Falha desconhecida")))
                .exchangeToMono(response -> handleVoidResponse("fail", url, response.statusCode()))
                .block();
    }

    private Mono<Void> handleVoidResponse(String action, String url, HttpStatusCode status) {
        if (status.isError()) {
            return Mono.error(new IllegalStateException("Backend " + action + " " + url + " retornou " + status));
        }
        return Mono.empty();
    }

    private PlaybookJob toJob(JobPayloadResponse response) {
        PlaybookJobType type = PlaybookJobType.valueOf(response.type());
        return new PlaybookJob(
                response.id(),
                type,
                response.workflowId(),
                response.resourceId(),
                response.payload(),
                response.createdAt()
        );
    }

    public enum PlaybookWorker {
        AI,
        FACEBOOK
    }

    private record JobPayloadResponse(long id,
                                      String type,
                                      JsonNode payload,
                                      Long workflowId,
                                      Long resourceId,
                                      Instant createdAt) {
    }
}

package com.marketinghub.worker.hypothesisframework;

import com.marketinghub.worker.util.UrlUtils;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class HypothesisFrameworkBackendClient {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public HypothesisFrameworkBackendClient(WebClient.Builder builder,
                                            @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                            @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<HypothesisFrameworkJobDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/hypothesis-framework/jobs/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        log.debug("Listing pending hypothesis framework jobs from {}", uri);
        List<HypothesisFrameworkJobDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .onErrorResume(err -> {
                    log.error("Failed to fetch pending hypothesis framework jobs", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        List<HypothesisFrameworkJobDto> jobs = payload != null ? payload : List.of();
        log.debug("Received {} pending hypothesis framework job(s) from backend", jobs.size());
        return jobs;
    }

    public HypothesisFrameworkJobDto claim(UUID jobId, String workerId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/hypothesis-framework/jobs/", jobId.toString(), "/claim");
        log.debug("Claiming hypothesis framework job {} using workerId={} at {}", jobId, workerId, url);
        return webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("workerId", workerId))
                .retrieve()
                .bodyToMono(HypothesisFrameworkJobDto.class)
                .onErrorResume(err -> {
                    log.debug("Failed to claim hypothesis framework job {}", jobId, err);
                    return Mono.empty();
                })
                .block();
    }

    public void complete(UUID jobId, HypothesisFrameworkJobCompletionPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/hypothesis-framework/jobs/", jobId.toString(), "/complete");
        webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(err -> {
                    log.error("Failed to complete hypothesis framework job {}", jobId, err);
                    return Mono.empty();
                })
                .block();
    }

    public void fail(UUID jobId, String errorMessage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/hypothesis-framework/jobs/", jobId.toString(), "/fail");
        webClient.post()
                .uri(url)
                .bodyValue(new HypothesisFrameworkJobFailurePayload(errorMessage))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(err -> {
                    log.error("Failed to fail hypothesis framework job {}", jobId, err);
                    return Mono.empty();
                })
                .block();
    }

    private Flux<HypothesisFrameworkJobDto> handleListResponse(String uri,
                                                               HttpStatusCode status,
                                                               org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(HypothesisFrameworkJobDto.class);
    }
}

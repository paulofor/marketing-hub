package com.marketinghub.worker.frameworkimage;

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
public class FrameworkImageBackendClient {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public FrameworkImageBackendClient(WebClient.Builder builder,
                                       @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                       @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<FrameworkImageJobDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/jobs/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        List<FrameworkImageJobDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .onErrorResume(err -> {
                    log.error("Failed to fetch pending framework image jobs", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    public List<FrameworkImageWebnizationPendingAssetDto> listPendingWebnization(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/assets/pending-webnization");
        String uri = url + "?limit=" + Math.max(1, limit);
        List<FrameworkImageWebnizationPendingAssetDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleWebnizationListResponse(uri, response.statusCode(), response))
                .collectList()
                .onErrorResume(err -> {
                    log.error("Failed to fetch pending framework image webnization assets", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    public FrameworkImageJobDto claim(UUID jobId, String workerId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/jobs/", jobId.toString(), "/claim");
        return webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("workerId", workerId))
                .retrieve()
                .bodyToMono(FrameworkImageJobDto.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void updateStage(UUID jobId, FrameworkImageJobStage stage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/jobs/", jobId.toString(), "/stage");
        webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("stage", stage.name()))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void complete(UUID jobId, FrameworkImageJobCompletionPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/jobs/", jobId.toString(), "/complete");
        webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void fail(UUID jobId, String errorMessage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/jobs/", jobId.toString(), "/fail");
        webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("errorMessage", errorMessage))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void markWebReady(Long assetId, String webUrl) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/framework-image/assets/", assetId.toString(), "/web-ready");
        webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("webUrl", webUrl))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private Flux<FrameworkImageJobDto> handleListResponse(String uri,
                                                           HttpStatusCode status,
                                                           org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(FrameworkImageJobDto.class);
    }

    private Flux<FrameworkImageWebnizationPendingAssetDto> handleWebnizationListResponse(
            String uri,
            HttpStatusCode status,
            org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(FrameworkImageWebnizationPendingAssetDto.class);
    }
}

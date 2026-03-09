package com.marketinghub.worker.targetingrequest;

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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class BackendTargetingRequestClient {
    private static final Logger log = LoggerFactory.getLogger(BackendTargetingRequestClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    public BackendTargetingRequestClient(WebClient.Builder builder,
                                         @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                         @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<TargetingRequestDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/targeting/requests/pending");
        String uri = url + "?limit=" + Math.max(limit, 1);
        log.info("Fetching pending targeting requests from {}", uri);
        List<TargetingRequestDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMapMany(body -> Mono.error(new IllegalStateException(
                                        errorMessage("GET", uri, status, body))));
                    }
                    return response.bodyToFlux(TargetingRequestDto.class);
                })
                .collectList()
                .onErrorResume(err -> {
                    log.error("Failed to fetch pending targeting requests", err);
                    return Mono.just(Collections.emptyList());
                })
                .block();
        return payload != null ? payload : List.of();
    }

    public void sendCandidates(UUID requestId, TargetingCandidateIngestionPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/targeting/", requestId.toString(), "/candidates");
        log.info("Sending {} candidates for request {}", payload != null && payload.candidates() != null ? payload.candidates().size() : 0, requestId);
        webClient.post()
                .uri(url)
                .bodyValue(payload)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        errorMessage("POST", url, status, body))));
                    }
                    return response.bodyToMono(Void.class);
                })
                .onErrorResume(err -> {
                    log.error("Failed to post candidates for request {}", requestId, err);
                    return Mono.empty();
                })
                .block();
    }

    private static String errorMessage(String method, String url, HttpStatusCode status, String body) {
        if (body != null && !body.isBlank()) {
            return "%s %s failed with status %s: %s".formatted(method, url, status, body);
        }
        return "%s %s failed with status %s".formatted(method, url, status);
    }
}

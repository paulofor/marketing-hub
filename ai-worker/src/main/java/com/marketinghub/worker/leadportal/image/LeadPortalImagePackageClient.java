package com.marketinghub.worker.leadportal.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.worker.util.UrlUtils;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LeadPortalImagePackageClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiPrefix;

    public LeadPortalImagePackageClient(
            WebClient.Builder builder,
            @Value("${lead-portal.backend.base-url:${backend.base-url}}") String baseUrl,
            @Value("${lead-portal.backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.baseUrl = baseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<ImagePackage> listRecentPackages() {
        String url = UrlUtils.joinPath(baseUrl, apiPrefix, "/worker/image-packages/recent");
        logRequest("GET", url);
        List<ImagePackagePayload> payload = webClient.get()
                .uri(url)
                .exchangeToFlux(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (status.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMapMany(body -> Mono.error(new LeadPortalWorkerException(
                                        errorMessage("GET", url, status, body))));
                    }
                    return response.bodyToFlux(ImagePackagePayload.class);
                })
                .collectList()
                .block(REQUEST_TIMEOUT);
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        return payload.stream().map(ImagePackagePayload::toDomain).toList();
    }

    public void markProcessing(long packageId) {
        postWithoutBody(UrlUtils.joinPath(baseUrl, apiPrefix, "/worker/image-packages/", String.valueOf(packageId), "/start"));
    }

    public void markFailed(long packageId, String reason) {
        FailureRequest request = new FailureRequest(reason);
        String url = UrlUtils.joinPath(baseUrl, apiPrefix, "/worker/image-packages/", String.valueOf(packageId), "/fail");
        postWithBody(url, request);
    }

    public void submitResults(long packageId, List<GeneratedImage> images, String model, String prompt) {
        ResultRequest request = new ResultRequest(images, model, prompt);
        String url = UrlUtils.joinPath(baseUrl, apiPrefix, "/worker/image-packages/", String.valueOf(packageId), "/results");
        postWithBody(url, request);
    }

    private void postWithoutBody(String url) {
        logRequest("POST", url);
        webClient.post()
                .uri(url)
                .exchangeToMono(response -> handleResponse(response.statusCode(), url, "POST"))
                .block(REQUEST_TIMEOUT);
    }

    private void postWithBody(String url, Object body) {
        logRequest("POST", url);
        webClient.post()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> handleResponse(response.statusCode(), url, "POST"))
                .block(REQUEST_TIMEOUT);
    }

    private Mono<Void> handleResponse(HttpStatusCode status, String url, String method) {
        if (status.isError()) {
            return Mono.error(new LeadPortalWorkerException(errorMessage(method, url, status, "")));
        }
        return Mono.empty();
    }

    private void logRequest(String method, String url) {
        if (log.isInfoEnabled()) {
            log.info("Calling marketing hub backend {} {}", method, url);
        }
    }

    private String errorMessage(String method, String url, HttpStatusCode status, String body) {
        return "%s %s responded with status %s and body '%s'".formatted(method, url, status, body);
    }

    public record ImagePackage(
            long id,
            UUID submissionId,
            String storedFileName,
            Integer plannedOutputs,
            Integer freeImages,
            String model,
            String prompt,
            String treatment) {}

    public record GeneratedImage(
            String storedFileName,
            String publicUrl,
            String model,
            String prompt,
            String source) {}

    private record ImagePackagePayload(
            long id,
            @JsonProperty("submission_id") UUID submissionId,
            @JsonProperty("stored_file_name") String storedFileName,
            @JsonProperty("planned_outputs") Integer plannedOutputs,
            @JsonProperty("free_images") Integer freeImages,
            String model,
            String prompt,
            String treatment) {

        ImagePackage toDomain() {
            return new ImagePackage(id, submissionId, storedFileName, plannedOutputs, freeImages, model, prompt, treatment);
        }
    }

    private record FailureRequest(String reason) {}

    private record ResultRequest(List<GeneratedImage> images, String model, String prompt) {}

    public static class LeadPortalWorkerException extends RuntimeException {
        public LeadPortalWorkerException(String message) {
            super(message);
        }
    }
}

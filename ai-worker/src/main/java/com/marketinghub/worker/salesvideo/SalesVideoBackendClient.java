package com.marketinghub.worker.salesvideo;

import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.JobClaimRequest;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.dto.JobProgressRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP responsável por acessar os endpoints internos do backend relativos ao módulo de vídeo.
 */
@Component
public class SalesVideoBackendClient {
    private static final Logger log = LoggerFactory.getLogger(SalesVideoBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final String internalPrefix;

    public SalesVideoBackendClient(WebClient.Builder builder,
                                   @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                   @Value("${backend.api-prefix:/api}") String apiPrefix,
                                   @Value("${backend.internal-prefix:/internal}") String internalPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.internalPrefix = internalPrefix;
    }

    public List<SalesVideoJobDto> listOpenAiJobs(SalesVideoStatus status,
                                                 SalesVideoJobType jobType,
                                                 int limit) {
        String base = UrlUtils.joinPath(backendBaseUrl, internalPrefix, "/ai/openai-jobs");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("limit", Math.max(1, Math.min(limit <= 0 ? 25 : limit, 200)));
        if (status != null) {
            uriBuilder.queryParam("status", status);
        }
        if (jobType != null) {
            uriBuilder.queryParam("type", jobType);
        }
        String uri = uriBuilder.toUriString();
        logBackendRequest("GET", uri);
        List<SalesVideoJobDto> jobs = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> {
                    HttpStatusCode code = response.statusCode();
                    if (code.value() == HttpStatus.NOT_FOUND.value()) {
                        return Flux.empty();
                    }
                    if (code.isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMapMany(body -> Mono.error(new SalesVideoBackendException(
                                        errorMessage("GET", uri, code, body))));
                    }
                    return response.bodyToFlux(SalesVideoJobDto.class);
                })
                .collectList()
                .block();
        return jobs == null ? List.of() : jobs;
    }

    public SalesVideoJobDto claimJob(Long jobId, String workerId, String message) {
        JobClaimRequest request = new JobClaimRequest();
        request.setWorkerId(workerId);
        request.setMessage(message);
        return postOpenAiJob(jobId, "/claim", request, SalesVideoJobDto.class);
    }

    public SalesVideoJobDto reportProgress(Long jobId,
                                           Integer progressPercent,
                                           SalesVideoStatus status,
                                           String message,
                                           String detailsJson) {
        JobProgressRequest request = new JobProgressRequest();
        request.setProgressPercent(progressPercent);
        request.setStatus(status);
        request.setMessage(message);
        request.setDetailsJson(detailsJson);
        return postOpenAiJob(jobId, "/progress", request, SalesVideoJobDto.class);
    }

    public SalesVideoJobDto completeJob(Long jobId, JobCompletionRequest request) {
        return postOpenAiJob(jobId, "/complete", request, SalesVideoJobDto.class);
    }

    public SalesVideoJobDto failJob(Long jobId, JobFailureRequest request) {
        return postOpenAiJob(jobId, "/fail", request, SalesVideoJobDto.class);
    }

    public SalesVideoProfileDto getProfile(Long profileId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/sales-videos/profiles/" + profileId);
        return get(url, SalesVideoProfileDto.class);
    }

    public ProductDto getProduct(Long productId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/products/" + productId);
        return get(url, ProductDto.class);
    }

    private <T> T postOpenAiJob(Long jobId, String suffix, Object body, Class<T> responseType) {
        String url = UrlUtils.joinPath(backendBaseUrl, internalPrefix, "/ai/openai-jobs")
                + "/" + jobId + suffix;
        logBackendRequest("POST", url);
        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> handleResponse(response, url, "POST", responseType))
                .block();
    }

    private <T> T get(String url, Class<T> responseType) {
        logBackendRequest("GET", url);
        return webClient.get()
                .uri(url)
                .exchangeToMono(response -> handleResponse(response, url, "GET", responseType))
                .block();
    }

    private <T> Mono<T> handleResponse(org.springframework.web.reactive.function.client.ClientResponse response,
                                       String url,
                                       String method,
                                       Class<T> responseType) {
        HttpStatusCode code = response.statusCode();
        if (code.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new SalesVideoBackendException(
                            errorMessage(method, url, code, body))));
        }
        if (responseType == null) {
            return Mono.empty();
        }
        return response.bodyToMono(responseType);
    }

    private void logBackendRequest(String method, String url) {
        log.debug("Calling backend {} {}", method, url);
    }

    private String errorMessage(String method, String url, HttpStatusCode status, String body) {
        return String.format("Backend %s %s failed with status %s and body %s",
                method, url, status, body);
    }

    /**
     * Exceção checked para padronizar erros vindos do backend.
     */
    public static class SalesVideoBackendException extends RuntimeException {
        public SalesVideoBackendException(String message) {
            super(message);
        }
    }
}

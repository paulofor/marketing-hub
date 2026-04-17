package com.marketinghub.videomanagement.client;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobExpirationPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.client.payload.JobHeartbeatPayload;
import com.marketinghub.videomanagement.client.payload.JobProgressPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.VideoJobObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Component
public class BackendVideoClient {
    private static final ParameterizedTypeReference<List<SalesVideoJob>> JOB_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final Logger log = LoggerFactory.getLogger(BackendVideoClient.class);
    private final WebClient webClient;
    private final VideoManagementProperties properties;
    private final VideoJobObservabilityService observabilityService;

    public BackendVideoClient(WebClient.Builder builder,
                              VideoManagementProperties properties,
                              VideoJobObservabilityService observabilityService) {
        this.webClient = builder
                .baseUrl(properties.getBackendBaseUrl().toString())
                .build();
        this.properties = properties;
        this.observabilityService = observabilityService;
    }

    public List<SalesVideoJob> fetchPendingJobs(int limit) {
        return fetchJobsByStatus(SalesVideoStatus.VIDEO_REQUESTED, limit);
    }

    public List<SalesVideoJob> fetchJobsByStatus(SalesVideoStatus status,
                                                 int limit) {
        try {
            return executeWithRetry("list jobs", () -> authorized(webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/internal/video/jobs")
                                    .queryParam("status", status)
                                    .queryParam("limit", Math.max(limit, 1))
                                    .build()))
                    .retrieve()
                    .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), response ->
                            mapError("Erro ao listar jobs", response))
                    .bodyToMono(JOB_LIST_TYPE)
                    .blockOptional()
                    .orElse(Collections.emptyList()));
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao consultar jobs no backend", ex);
            throw wrap("Falha ao consultar jobs no backend", ex);
        }
    }

    public SalesVideoJob fetchJob(Long jobId) {
        try {
            return executeWithRetry("fetch job " + jobId, () -> authorized(webClient.get()
                            .uri("/internal/video/jobs/{jobId}", jobId))
                    .retrieve()
                    .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), response ->
                            mapError("Erro ao carregar job %d".formatted(jobId), response))
                    .bodyToMono(SalesVideoJob.class)
                    .blockOptional()
                    .orElseThrow(() -> new BackendIntegrationException(
                            "Job de vídeo não encontrado: " + jobId, 404)));
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Falha ao consultar job %d".formatted(jobId), ex);
        }
    }

    public SalesVideoProfile fetchProfile(Long profileId) {
        try {
            return executeWithRetry("fetch profile " + profileId, () -> authorized(webClient.get()
                            .uri("/api/sales-videos/profiles/{profileId}", profileId))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            mapError("Erro ao carregar perfil %d".formatted(profileId), response))
                    .bodyToMono(SalesVideoProfile.class)
                    .blockOptional()
                    .orElseThrow(() -> new BackendIntegrationException(
                            "Perfil de vídeo não encontrado: " + profileId, 404)));
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Falha ao consultar perfil %d".formatted(profileId), ex);
        }
    }

    public SalesVideoJob claimJob(Long jobId, JobClaimPayload payload) {
        return postForJob("/internal/video/jobs/{jobId}/claim", jobId, payload);
    }

    public void reportProgress(Long jobId, JobProgressPayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/progress", jobId, payload);
    }

    public void reportHeartbeat(Long jobId, JobHeartbeatPayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/heartbeat", jobId, payload);
    }

    public void completeJob(Long jobId, JobCompletionPayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/complete", jobId, payload);
    }

    public void failJob(Long jobId, JobFailurePayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/fail", jobId, payload);
    }

    public void expireJob(Long jobId, JobExpirationPayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/expired", jobId, payload);
    }

    private SalesVideoJob postForJob(String path,
                                     Long jobId,
                                     Object payload) {
        try {
            return executeWithRetry("post " + path + " job " + jobId, () -> authorized(webClient.post()
                            .uri(path, jobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            mapError("Erro ao atualizar job %d".formatted(jobId), response))
                    .bodyToMono(SalesVideoJob.class)
                    .block());
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Falha ao atualizar job %d".formatted(jobId), ex);
        }
    }

    private void postIgnoringBody(String path,
                                  Long jobId,
                                  Object payload) {
        try {
            executeWithRetry("post " + path + " job " + jobId, () -> {
                authorized(webClient.post()
                            .uri(path, jobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            mapError("Erro ao atualizar job %d".formatted(jobId), response))
                    .toBodilessEntity()
                    .block();
                return null;
            });
        } catch (BackendIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Falha ao atualizar job %d".formatted(jobId), ex);
        }
    }

    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> spec) {
        if (StringUtils.hasText(properties.getAuthToken())) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthToken());
        }
        return spec;
    }

    private BackendIntegrationException wrap(String message, Exception ex) {
        return new BackendIntegrationException(message, ex);
    }

    private Mono<? extends Throwable> mapError(String operation,
                                               ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new BackendIntegrationException(operation + ": " + body,
                        response.statusCode().value()));
    }

    private <T> T executeWithRetry(String operation, Supplier<T> action) {
        int maxAttempts = properties.getJobs().getBackendCallMaxAttempts();
        long backoffMs = properties.getJobs().getBackendCallBackoff().toMillis();
        BackendIntegrationException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (BackendIntegrationException ex) {
                lastError = ex;
                if (!shouldRetry(ex.getStatusCode()) || attempt == maxAttempts) {
                    throw ex;
                }
                observabilityService.incrementBackendRetry(operation, ex.getStatusCode());
                log.warn("Tentativa {}/{} falhou em {} (status={}): {}",
                        attempt, maxAttempts, operation, ex.getStatusCode(), ex.getMessage());
                sleep(backoffMs);
            } catch (WebClientResponseException ex) {
                lastError = new BackendIntegrationException(operation + ": " + ex.getResponseBodyAsString(),
                        ex.getStatusCode().value(),
                        ex);
                if (!shouldRetry(ex.getStatusCode().value()) || attempt == maxAttempts) {
                    throw lastError;
                }
                observabilityService.incrementBackendRetry(operation, ex.getStatusCode().value());
                log.warn("Tentativa {}/{} falhou em {} (status={}): {}",
                        attempt, maxAttempts, operation, ex.getStatusCode().value(), ex.getMessage());
                sleep(backoffMs);
            } catch (Exception ex) {
                lastError = new BackendIntegrationException(operation + ": " + ex.getMessage(), ex);
                if (attempt == maxAttempts) {
                    throw lastError;
                }
                observabilityService.incrementBackendRetry(operation, null);
                log.warn("Tentativa {}/{} falhou em {}: {}", attempt, maxAttempts, operation, ex.getMessage());
                sleep(backoffMs);
            }
        }
        throw lastError == null ? new BackendIntegrationException("Falha inesperada em " + operation) : lastError;
    }

    private boolean shouldRetry(Integer statusCode) {
        if (statusCode == null) {
            return true;
        }
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(millis, 0));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BackendIntegrationException("Thread interrompida durante retry com backend", ex);
        }
    }
}

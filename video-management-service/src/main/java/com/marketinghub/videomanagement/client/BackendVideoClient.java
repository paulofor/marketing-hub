package com.marketinghub.videomanagement.client;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.client.payload.JobProgressPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class BackendVideoClient {
    private static final ParameterizedTypeReference<List<SalesVideoJob>> JOB_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final Logger log = LoggerFactory.getLogger(BackendVideoClient.class);
    private final WebClient webClient;
    private final VideoManagementProperties properties;

    public BackendVideoClient(WebClient.Builder builder,
                              VideoManagementProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getBackendBaseUrl().toString())
                .build();
        this.properties = properties;
    }

    public List<SalesVideoJob> fetchPendingJobs(int limit) {
        try {
            return authorized(webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/internal/video/jobs")
                                    .queryParam("status", SalesVideoStatus.VIDEO_REQUESTED)
                                    .queryParam("limit", Math.max(limit, 1))
                                    .build()))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new BackendIntegrationException(
                                            "Erro ao listar jobs: " + body)))
                    .bodyToMono(JOB_LIST_TYPE)
                    .blockOptional()
                    .orElse(Collections.emptyList());
        } catch (Exception ex) {
            log.error("Falha ao consultar jobs no backend", ex);
            throw wrap("Falha ao consultar jobs no backend", ex);
        }
    }

    public SalesVideoProfile fetchProfile(Long profileId) {
        try {
            return authorized(webClient.get()
                            .uri("/api/sales-videos/profiles/{profileId}", profileId))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new BackendIntegrationException(
                                            "Erro ao carregar perfil %d: %s".formatted(profileId, body))))
                    .bodyToMono(SalesVideoProfile.class)
                    .blockOptional()
                    .orElseThrow(() -> new BackendIntegrationException(
                            "Perfil de vídeo não encontrado: " + profileId));
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

    public void completeJob(Long jobId, JobCompletionPayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/complete", jobId, payload);
    }

    public void failJob(Long jobId, JobFailurePayload payload) {
        postIgnoringBody("/internal/video/jobs/{jobId}/fail", jobId, payload);
    }

    private SalesVideoJob postForJob(String path,
                                     Long jobId,
                                     Object payload) {
        try {
            return authorized(webClient.post()
                            .uri(path, jobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new BackendIntegrationException(
                                            "Erro ao atualizar job %d: %s".formatted(jobId, body))))
                    .bodyToMono(SalesVideoJob.class)
                    .block();
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
            authorized(webClient.post()
                            .uri(path, jobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new BackendIntegrationException(
                                            "Erro ao atualizar job %d: %s".formatted(jobId, body))))
                    .toBodilessEntity()
                    .block();
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
}

package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobExpirationPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.client.payload.JobHeartbeatPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.VideoAssetUploader.UploadedAssets;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.VideoProvider;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class VideoJobProcessor {
    private final Logger log = LoggerFactory.getLogger(VideoJobProcessor.class);
    private final BackendVideoClient backendClient;
    private final ProviderRegistry providerRegistry;
    private final VideoAssetUploader assetUploader;
    private final VideoJobObservabilityService observabilityService;
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;

    public VideoJobProcessor(BackendVideoClient backendClient,
                             ProviderRegistry providerRegistry,
                             VideoAssetUploader assetUploader,
                             VideoJobObservabilityService observabilityService,
                             VideoManagementProperties properties,
                             ObjectMapper objectMapper) {
        this.backendClient = backendClient;
        this.providerRegistry = providerRegistry;
        this.assetUploader = assetUploader;
        this.observabilityService = observabilityService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void process(SalesVideoJob job) {
        try (AutoCloseable ignored = putMdc(job)) {
            log.info("Processando job {} para profile {}", job.id(), job.profileId());
            if (!claimJob(job)) {
                return;
            }
            observabilityService.incrementJobsDispatched(job.providerName());
            backendClient.reportHeartbeat(job.id(), new JobHeartbeatPayload(
                    "Job em execução pelo worker " + properties.getWorkerId(), null));
            SalesVideoProfile profile = loadProfile(job);
            VideoProvider provider = providerRegistry.resolve(job)
                    .orElseThrow(() -> new VideoProviderException("Nenhum provider configurado para o job"));
            ProviderArtifacts artifacts = provider.render(job, profile,
                    new VideoJobProgressReporter(backendClient, job.id()));
            if (artifacts.videoFile() == null) {
                throw new VideoProviderException("Provider não retornou o asset principal de vídeo");
            }
            UploadedAssets uploadedAssets = assetUploader.uploadAssets(job, artifacts);
            String metadataJson = serializeMetadata(artifacts.metadata());
            backendClient.completeJob(job.id(), new JobCompletionPayload(
                    SalesVideoStatus.VIDEO_READY,
                    uploadedAssets.videoAssetId(),
                    uploadedAssets.posterAssetId(),
                    uploadedAssets.captionAssetId(),
                    artifacts.providerJobId(),
                    metadataJson,
                    "Vídeo processado com sucesso",
                    metadataJson));
            observabilityService.incrementJobsCompleted(job.providerName());
            observabilityService.recordRenderLatency(job.providerName(), computeLatency(job));
            log.info("Job {} concluído com vídeo {}", job.id(), uploadedAssets.videoAssetId());
        } catch (VideoProviderException ex) {
            log.warn("Falha ao processar job {}: {}", job.id(), ex.getMessage());
            observabilityService.incrementJobsFailed(job.providerName(), ex.getCode());
            if (isExpiredFailure(ex)) {
                observabilityService.incrementAssetExpired(job.providerName());
                backendClient.expireJob(job.id(), new JobExpirationPayload(
                        "Provider informou asset expirado",
                        ex.getMessage()));
            } else {
                String failureCode = ex.getCode();
                backendClient.failJob(job.id(), new JobFailurePayload(
                        failureCode,
                        buildFailureDetail(ex),
                        SalesVideoStatus.VIDEO_FAILED,
                        ex.getMessage(),
                        isRetryableFailure(failureCode),
                        toRetryReason(failureCode)));
            }
        } catch (BackendIntegrationException ex) {
            if (isDuplicateClaim(ex) || isNotFound(ex)) {
                log.info("Job {} não será processado neste worker (status={}): {}",
                        job.id(), ex.getStatusCode(), ex.getMessage());
                observabilityService.incrementClaimConflict(job.providerName());
                return;
            }
            log.error("Falha de integração com backend ao processar job {}: {}", job.id(), ex.getMessage());
            safeFailJob(job.id(), "BACKEND_INTEGRATION_ERROR", ex.getMessage(), "Falha ao comunicar com backend");
        } catch (Exception ex) {
            log.error("Erro inesperado ao processar job {}", job.id(), ex);
            safeFailJob(job.id(), "VIDEO_MODULE_ERROR", ex.getMessage(), "Falha inesperada ao processar job");
        }
    }

    private boolean claimJob(SalesVideoJob job) {
        try {
            backendClient.claimJob(job.id(), new JobClaimPayload(properties.getWorkerId(),
                    "Claim automático pelo video-management-service"));
            return true;
        } catch (BackendIntegrationException ex) {
            if (isDuplicateClaim(ex) || isNotFound(ex)) {
                log.info("Claim recusado para job {} (status={}): {}", job.id(), ex.getStatusCode(), ex.getMessage());
                return false;
            }
            throw ex;
        }
    }

    private boolean isExpiredFailure(Exception ex) {
        if (ex instanceof VideoProviderException providerException) {
            return "PROVIDER_ASSET_EXPIRED".equalsIgnoreCase(providerException.getCode());
        }
        return false;
    }

    private SalesVideoProfile loadProfile(SalesVideoJob job) {
        if (job.profileId() == null) {
            throw new VideoProviderException("Job não está vinculado a um profile");
        }
        return backendClient.fetchProfile(job.profileId());
    }

    private void safeFailJob(Long jobId,
                             String code,
                             String detail,
                             String message) {
        try {
            backendClient.failJob(jobId, new JobFailurePayload(
                    code,
                    detail,
                    SalesVideoStatus.VIDEO_FAILED,
                    message,
                    false,
                    "OTHER"));
        } catch (Exception failEx) {
            log.error("Falha ao registrar erro do job {} no backend: {}", jobId, failEx.getMessage());
        }
    }

    private boolean isDuplicateClaim(BackendIntegrationException ex) {
        return ex.getStatusCode() != null && ex.getStatusCode() == 409;
    }

    private boolean isNotFound(BackendIntegrationException ex) {
        return ex.getStatusCode() != null && ex.getStatusCode() == 404;
    }

    private String buildFailureDetail(VideoProviderException ex) {
        String reason = ex.getCode();
        boolean retryable = isRetryableFailure(reason);
        return "retryable=%s;code=%s;message=%s".formatted(retryable, reason, ex.getMessage());
    }

    private boolean isRetryableFailure(String reason) {
        return "PROVIDER_TIMEOUT".equalsIgnoreCase(reason) || "PROVIDER_RATE_LIMIT".equalsIgnoreCase(reason);
    }

    private String toRetryReason(String failureCode) {
        if ("PROVIDER_ASSET_EXPIRED".equalsIgnoreCase(failureCode)) {
            return "ASSET_EXPIRED";
        }
        if (isRetryableFailure(failureCode)) {
            return "PROVIDER_FAILURE";
        }
        return "OTHER";
    }

    private AutoCloseable putMdc(SalesVideoJob job) {
        MDC.put("jobId", asString(job.id()));
        MDC.put("profileId", asString(job.profileId()));
        MDC.put("provider", valueOrUnknown(job.providerName()));
        MDC.put("providerJobId", valueOrUnknown(job.providerJobId()));
        MDC.put("tenant", valueOrUnknown(job.tenantId()));
        return MDC::clear;
    }

    private String asString(Long value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private Duration computeLatency(SalesVideoJob job) {
        Instant requestedAt = job.requestedAt();
        if (requestedAt == null) {
            return null;
        }
        return Duration.between(requestedAt, Instant.now());
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new VideoProviderException("Falha ao serializar metadata do provider", ex);
        }
    }
}

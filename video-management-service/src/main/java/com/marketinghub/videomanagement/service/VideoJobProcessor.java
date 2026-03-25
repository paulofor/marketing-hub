package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.VideoAssetUploader.UploadedAssets;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.VideoProvider;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class VideoJobProcessor {
    private final Logger log = LoggerFactory.getLogger(VideoJobProcessor.class);
    private final BackendVideoClient backendClient;
    private final ProviderRegistry providerRegistry;
    private final VideoAssetUploader assetUploader;
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;

    public VideoJobProcessor(BackendVideoClient backendClient,
                             ProviderRegistry providerRegistry,
                             VideoAssetUploader assetUploader,
                             VideoManagementProperties properties,
                             ObjectMapper objectMapper) {
        this.backendClient = backendClient;
        this.providerRegistry = providerRegistry;
        this.assetUploader = assetUploader;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void process(SalesVideoJob job) {
        log.info("Processando job {} para profile {}", job.id(), job.profileId());
        try {
            backendClient.claimJob(job.id(), new JobClaimPayload(properties.getWorkerId(),
                    "Claim automático pelo video-management-service"));
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
            log.info("Job {} concluído com vídeo {}", job.id(), uploadedAssets.videoAssetId());
        } catch (VideoProviderException | BackendIntegrationException ex) {
            log.warn("Falha ao processar job {}: {}", job.id(), ex.getMessage());
            backendClient.failJob(job.id(), new JobFailurePayload(
                    "VIDEO_PROVIDER_ERROR",
                    ex.getMessage(),
                    SalesVideoStatus.VIDEO_FAILED,
                    ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erro inesperado ao processar job {}", job.id(), ex);
            backendClient.failJob(job.id(), new JobFailurePayload(
                    "VIDEO_MODULE_ERROR",
                    ex.getMessage(),
                    SalesVideoStatus.VIDEO_FAILED,
                    "Falha inesperada ao processar job"));
        }
    }

    private SalesVideoProfile loadProfile(SalesVideoJob job) {
        if (job.profileId() == null) {
            throw new VideoProviderException("Job não está vinculado a um profile");
        }
        return backendClient.fetchProfile(job.profileId());
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

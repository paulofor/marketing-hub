package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetResponse;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VideoAssetUploader {
    private final VideoAssetClient assetClient;
    private final ObjectMapper objectMapper;

    public VideoAssetUploader(VideoAssetClient assetClient, ObjectMapper objectMapper) {
        this.assetClient = assetClient;
        this.objectMapper = objectMapper;
    }

    public UploadedAssets uploadAssets(SalesVideoJob job, ProviderArtifacts artifacts) {
        Long videoAssetId = upload(job, artifacts, artifacts.videoFile());
        Long posterAssetId = upload(job, artifacts, artifacts.posterFile());
        Long captionAssetId = upload(job, artifacts, artifacts.captionFile());
        return new UploadedAssets(videoAssetId, posterAssetId, captionAssetId);
    }

    private Long upload(SalesVideoJob job,
                        ProviderArtifacts artifacts,
                        ProviderFile file) {
        if (file == null) {
            return null;
        }
        String metadataJson = serializeMetadata(job, artifacts, file);
        AssetResponse response = assetClient.uploadAsset(file, metadataJson);
        return response != null ? response.id() : null;
    }

    private String serializeMetadata(SalesVideoJob job,
                                     ProviderArtifacts artifacts,
                                     ProviderFile file) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("job_id", job.id());
        metadata.put("profile_id", job.profileId());
        metadata.put("script_id", job.scriptId());
        metadata.put("role", file.role().name());
        metadata.put("provider_job_id", artifacts.providerJobId());
        metadata.put("file_name", file.fileName());
        if (!CollectionUtils.isEmpty(artifacts.metadata())) {
            metadata.put("provider_metadata", artifacts.metadata());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new BackendIntegrationException("Não foi possível serializar metadata do asset", ex);
        }
    }

    public record UploadedAssets(Long videoAssetId,
                                 Long posterAssetId,
                                 Long captionAssetId) {
    }
}

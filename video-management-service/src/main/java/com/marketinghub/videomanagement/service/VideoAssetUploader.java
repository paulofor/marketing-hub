package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publica artefatos de vídeo no storage final e monta referências para callback ao backend.
 */
@Component
public class VideoAssetUploader {
    private final VideoAssetClient assetClient;
    private final VideoR2StorageService storageService;
    private final ObjectMapper objectMapper;

    public VideoAssetUploader(VideoAssetClient assetClient,
                              VideoR2StorageService storageService,
                              ObjectMapper objectMapper) {
        this.assetClient = assetClient;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    /** Envia os artefatos do provider para R2 e retorna IDs legados ou URLs finais. */
    public UploadedAssets uploadAssets(SalesVideoJob job, ProviderArtifacts artifacts) {
        StoredAsset videoAsset = upload(job, artifacts, artifacts.videoFile());
        StoredAsset posterAsset = upload(job, artifacts, artifacts.posterFile());
        StoredAsset captionAsset = upload(job, artifacts, artifacts.captionFile());
        return new UploadedAssets(
                videoAsset.assetId(),
                posterAsset.assetId(),
                captionAsset.assetId(),
                videoAsset.assetUrl(),
                posterAsset.assetUrl(),
                captionAsset.assetUrl());
    }

    private StoredAsset upload(SalesVideoJob job,
                               ProviderArtifacts artifacts,
                               ProviderFile file) {
        if (file == null) {
            return StoredAsset.empty();
        }
        VideoR2StorageService.StoredVideoAsset storedAsset = storageService.store(job.id(), file);
        if (storedAsset != null && storedAsset.publicUrl() != null) {
            return new StoredAsset(null, storedAsset.publicUrl());
        }
        String metadataJson = serializeMetadata(job, artifacts, file);
        var response = assetClient.uploadAsset(file, metadataJson);
        return new StoredAsset(response != null ? response.id() : null, null);
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

    /**
     * Referências finais dos assets enviados ao backend.
     */
    public record UploadedAssets(Long videoAssetId,
                                 Long posterAssetId,
                                 Long captionAssetId,
                                 String videoAssetUrl,
                                 String posterAssetUrl,
                                 String captionAssetUrl) {
    }

    /**
     * Resultado interno de armazenamento de um artefato individual.
     */
    private record StoredAsset(Long assetId, String assetUrl) {
        private static StoredAsset empty() {
            return new StoredAsset(null, null);
        }
    }
}

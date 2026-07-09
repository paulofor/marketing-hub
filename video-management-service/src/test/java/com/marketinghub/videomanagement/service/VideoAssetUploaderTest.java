package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoAssetUploaderTest {

    @Mock
    private VideoAssetClient assetClient;

    @Mock
    private VideoR2StorageService storageService;

    private VideoAssetUploader uploader;

    @BeforeEach
    void setUp() {
        uploader = new VideoAssetUploader(assetClient, storageService, new ObjectMapper());
    }

    @Test
    void shouldStoreAllArtifactsInR2AndReturnUrls() {
        SalesVideoJob job = sampleJob();
        ProviderFile video = new ProviderFile("video.mp4", MediaType.valueOf("video/mp4"), AssetType.VIDEO,
                ProviderAssetRole.VIDEO, new byte[]{1});
        ProviderFile poster = new ProviderFile("poster.png", MediaType.IMAGE_PNG, AssetType.IMAGE,
                ProviderAssetRole.POSTER, new byte[]{2});
        ProviderFile captions = new ProviderFile("captions.vtt", MediaType.valueOf("text/vtt"), AssetType.CAPTION,
                ProviderAssetRole.CAPTION, new byte[]{3});
        ProviderArtifacts artifacts = new ProviderArtifacts("stub-1", video, poster, captions, Map.of("key", "value"));
        when(storageService.store(job.id(), video)).thenReturn(
                new VideoR2StorageService.StoredVideoAsset("https://cdn.test/video.mp4", "video-key", 1, "video/mp4"));
        when(storageService.store(job.id(), poster)).thenReturn(
                new VideoR2StorageService.StoredVideoAsset("https://cdn.test/poster.png", "poster-key", 1, "image/png"));
        when(storageService.store(job.id(), captions)).thenReturn(
                new VideoR2StorageService.StoredVideoAsset("https://cdn.test/captions.vtt", "caption-key", 1, "text/vtt"));

        VideoAssetUploader.UploadedAssets result = uploader.uploadAssets(job, artifacts);

        assertThat(result.videoAssetUrl()).isEqualTo("https://cdn.test/video.mp4");
        assertThat(result.posterAssetUrl()).isEqualTo("https://cdn.test/poster.png");
        assertThat(result.captionAssetUrl()).isEqualTo("https://cdn.test/captions.vtt");
        verify(assetClient, never()).uploadAsset(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private SalesVideoJob sampleJob() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "STUB",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_REQUESTED,
                1,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }
}

package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.VideoAssetUploader.UploadedAssets;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import com.marketinghub.videomanagement.service.provider.VideoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoJobProcessorTest {

    @Mock
    private BackendVideoClient backendClient;

    @Mock
    private ProviderRegistry providerRegistry;

    @Mock
    private VideoAssetUploader assetUploader;

    @Mock
    private VideoProvider videoProvider;

    @Captor
    private ArgumentCaptor<JobCompletionPayload> completionCaptor;

    @Captor
    private ArgumentCaptor<JobFailurePayload> failureCaptor;

    private VideoJobProcessor processor;

    @BeforeEach
    void setUp() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setWorkerId("worker-test");
        processor = new VideoJobProcessor(backendClient, providerRegistry, assetUploader, properties, new ObjectMapper());
    }

    @Test
    void shouldCompleteJobWhenProviderSucceeds() {
        SalesVideoJob job = job();
        SalesVideoProfile profile = profile();
        ProviderFile videoFile = new ProviderFile("video.mp4", MediaType.valueOf("video/mp4"), AssetType.VIDEO,
                ProviderAssetRole.VIDEO, new byte[]{1});
        ProviderArtifacts artifacts = new ProviderArtifacts("stub-1", videoFile, null, null, Map.of("key", "value"));
        when(backendClient.fetchProfile(2L)).thenReturn(profile);
        when(providerRegistry.resolve(job)).thenReturn(Optional.of(videoProvider));
        when(videoProvider.render(any(), any(), any())).thenReturn(artifacts);
        when(assetUploader.uploadAssets(job, artifacts)).thenReturn(new UploadedAssets(20L, null, null));

        processor.process(job);

        verify(backendClient).claimJob(org.mockito.Mockito.eq(job.id()),
                org.mockito.ArgumentMatchers.any(JobClaimPayload.class));
        verify(backendClient).completeJob(org.mockito.Mockito.eq(job.id()), completionCaptor.capture());
        JobCompletionPayload payload = completionCaptor.getValue();
        assertThat(payload.assetId()).isEqualTo(20L);
        assertThat(payload.status()).isEqualTo(SalesVideoStatus.VIDEO_READY);
        verify(backendClient, never()).failJob(any(), any());
    }

    @Test
    void shouldFailJobWhenNoProviderIsFound() {
        SalesVideoJob job = job();
        when(backendClient.fetchProfile(2L)).thenReturn(profile());
        when(providerRegistry.resolve(job)).thenReturn(Optional.empty());

        processor.process(job);

        verify(backendClient).failJob(any(), failureCaptor.capture());
        assertThat(failureCaptor.getValue().failureCode()).isEqualTo("VIDEO_PROVIDER_ERROR");
    }

    @Test
    void shouldSkipProcessingWhenClaimIsDuplicated() {
        SalesVideoJob job = job();
        when(backendClient.claimJob(any(), any()))
                .thenThrow(new BackendIntegrationException("claim conflict", 409));

        processor.process(job);

        verify(backendClient, never()).fetchProfile(any());
        verify(backendClient, never()).failJob(any(), any());
        verify(backendClient, never()).completeJob(any(), any());
    }

    private SalesVideoJob job() {
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

    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "script text",
                "hook",
                "cta",
                "caption",
                null,
                "MANUAL",
                "gpt",
                "prompt",
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                1L,
                null,
                "SHORT",
                "Título",
                "Persona",
                "Estilo",
                "Voz",
                "pt-BR",
                60,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}

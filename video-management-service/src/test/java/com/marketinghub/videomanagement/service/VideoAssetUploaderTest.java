package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Valida o envio de todos os artefatos produzidos pelo pipeline para o backend. */
@ExtendWith(MockitoExtension.class)
class VideoAssetUploaderTest {

    @Mock
    private VideoAssetClient assetClient;

    @Captor
    private ArgumentCaptor<String> metadataCaptor;

    private VideoAssetUploader uploader;

    /** Inicializa o componente sob teste com as dependências simuladas. */
    @BeforeEach
    void setUp() {
        uploader = new VideoAssetUploader(assetClient, new ObjectMapper());
    }

    /** Confirma que vídeo, pôster, legendas e auditorias recebem metadados rastreáveis. */
    @Test
    void shouldUploadAllArtifactsAndReturnIds() {
        SalesVideoJob job = sampleJob();
        ProviderFile video = new ProviderFile("video.mp4", MediaType.valueOf("video/mp4"), AssetType.VIDEO,
                ProviderAssetRole.VIDEO, new byte[]{1});
        ProviderFile poster = new ProviderFile("poster.png", MediaType.IMAGE_PNG, AssetType.IMAGE,
                ProviderAssetRole.POSTER, new byte[]{2});
        ProviderFile captions = new ProviderFile("captions.vtt", MediaType.valueOf("text/vtt"), AssetType.CAPTION,
                ProviderAssetRole.CAPTION, new byte[]{3});
        ProviderFile auditAudio = new ProviderFile("tts.mp3", MediaType.valueOf("audio/mpeg"), AssetType.AUDIO,
                ProviderAssetRole.AUDIO_AUDIT, new byte[]{4});
        ProviderArtifacts artifacts = new ProviderArtifacts(
                "stub-1", video, poster, captions, Map.of("key", "value"), List.of(auditAudio));
        when(assetClient.uploadAsset(any(), any())).thenReturn(
                new AssetResponse(11L, AssetType.VIDEO, null, null, null, null, null),
                new AssetResponse(12L, AssetType.IMAGE, null, null, null, null, null),
                new AssetResponse(13L, AssetType.CAPTION, null, null, null, null, null),
                new AssetResponse(14L, AssetType.AUDIO, null, null, null, null, null));

        VideoAssetUploader.UploadedAssets result = uploader.uploadAssets(job, artifacts);

        assertThat(result.videoAssetId()).isEqualTo(11L);
        assertThat(result.posterAssetId()).isEqualTo(12L);
        assertThat(result.captionAssetId()).isEqualTo(13L);
        verify(assetClient, org.mockito.Mockito.times(4))
                .uploadAsset(any(), metadataCaptor.capture());
        assertThat(metadataCaptor.getAllValues().get(0)).contains(
                "\"job_id\":1",
                "\"size_bytes\":1",
                "\"sha256\":\"4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a\"");
        assertThat(metadataCaptor.getAllValues().get(3))
                .contains(
                        "\"role\":\"AUDIO_AUDIT\"",
                        "\"file_name\":\"tts.mp3\"",
                        "\"sha256\":\"e52d9c508c502347344d8c07ad91cbd6068afc75ff6292f062a09ca381c89e71\"");
    }

    /** Monta um job mínimo para os cenários de upload de artefatos. */
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

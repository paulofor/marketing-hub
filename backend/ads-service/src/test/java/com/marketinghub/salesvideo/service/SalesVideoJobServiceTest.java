package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.marketinghub.salesvideo.exception.VideoModuleException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Valida as regras de negocio dos jobs do modulo de video.
 */
@ExtendWith(MockitoExtension.class)
class SalesVideoJobServiceTest {

    @Mock
    private SalesVideoJobRepository jobRepository;

    @Mock
    private SalesVideoJobEventRepository eventRepository;

    @Mock
    private SalesVideoProfileRepository profileRepository;

    @Mock
    private SalesVideoScriptRepository scriptRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private SalesVideoReprocessPolicy reprocessPolicy;
    @Mock
    private SalesVideoCompletedRenderAssetSync completedRenderAssetSync;

    private SalesVideoJobService service;

    /** Inicializa o service com dependencias simuladas para cada teste. */
    @BeforeEach
    void setUp() {
        service = new SalesVideoJobService(jobRepository,
                eventRepository,
                profileRepository,
                scriptRepository,
                assetRepository,
                reprocessPolicy,
                completedRenderAssetSync,
                new ObjectMapper());
    }

    /** Garante que os jobs de um perfil sao retornados em contrato de leitura. */
    @Test
    void shouldListJobsByProfile() {
        long profileId = 10L;
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .id(profileId)
                .build();
        given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
        SalesVideoJob job = SalesVideoJob.builder()
                .id(55L)
                .profile(profile)
                .jobType(SalesVideoJobType.RENDER)
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .requestedAt(Instant.parse("2024-01-01T10:15:30Z"))
                .build();
        given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId))
                .willReturn(List.of(job));

        List<SalesVideoJobDto> result = service.listJobsByProfile(profileId);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(SalesVideoJobDto::getId)
                .isEqualTo(55L);
        assertThat(result.get(0).getStatus()).isEqualTo(SalesVideoStatus.VIDEO_REQUESTED);
    }

    /** Garante erro de negocio quando o perfil solicitado nao existe. */
    @Test
    void shouldRejectWhenProfileDoesNotExist() {
        long missingId = 404L;
        given(profileRepository.findById(missingId)).willReturn(Optional.empty());

        assertThrows(VideoModuleException.class, () -> service.listJobsByProfile(missingId));
    }

    /** Bloqueia render que terminou tecnicamente, mas ficou curto demais para o perfil comercial. */
    @Test
    void shouldFailRenderWhenAuditedDurationIsShorterThanCommercialTarget() {
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .id(6L)
                .targetDurationSeconds(30)
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .build();
        SalesVideoJob job = SalesVideoJob.builder()
                .id(20430L)
                .profile(profile)
                .jobType(SalesVideoJobType.RENDER)
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .status(SalesVideoStatus.VIDEO_PROCESSING)
                .build();
        JobCompletionRequest request = new JobCompletionRequest();
        request.setStatus(SalesVideoStatus.VIDEO_READY);
        request.setMetadataJson("{\"duration_seconds\":8,\"resolution\":\"720p\"}");
        request.setMessage("Render concluido pelo provider");
        given(jobRepository.findById(20430L)).willReturn(Optional.of(job));
        given(jobRepository.save(job)).willReturn(job);

        SalesVideoJobDto result = service.complete(20430L, request);

        assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
        assertThat(job.getFailureCode()).isEqualTo("RENDER_DURATION_SHORT");
        assertThat(job.getFailureDetail()).contains("8s").contains("30s");
        assertThat(profile.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
        verify(completedRenderAssetSync, never()).syncCompletedRender(
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any());
    }

    /** Aceita render quando a duração auditada atende a tolerância comercial do perfil. */
    @Test
    void shouldAcceptRenderWhenAuditedDurationMatchesCommercialTargetTolerance() {
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .id(6L)
                .targetDurationSeconds(30)
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .build();
        SalesVideoJob job = SalesVideoJob.builder()
                .id(20431L)
                .profile(profile)
                .jobType(SalesVideoJobType.RENDER)
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .status(SalesVideoStatus.VIDEO_PROCESSING)
                .build();
        JobCompletionRequest request = new JobCompletionRequest();
        request.setStatus(SalesVideoStatus.VIDEO_READY);
        request.setMetadataJson("{\"duration_seconds\":28,\"resolution\":\"720p\"}");
        given(jobRepository.findById(20431L)).willReturn(Optional.of(job));
        given(jobRepository.save(job)).willReturn(job);

        SalesVideoJobDto result = service.complete(20431L, request);

        assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
        assertThat(job.getFailureCode()).isNull();
        verify(completedRenderAssetSync).syncCompletedRender(
                org.mockito.Mockito.eq(job),
                org.mockito.Mockito.eq(request),
                org.mockito.Mockito.eq(28),
                org.mockito.Mockito.eq("720p"));
    }
}

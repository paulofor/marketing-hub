package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.salesvideo.LandingVideoSlot;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

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
    private ExperimentVideoAssetRepository experimentVideoAssetRepository;

    @Mock
    private LandingVideoSlotRepository landingVideoSlotRepository;

    @Mock
    private SalesVideoReprocessPolicy reprocessPolicy;

    private SalesVideoJobService service;

    @BeforeEach
    void setUp() {
        service = new SalesVideoJobService(jobRepository,
                eventRepository,
                profileRepository,
                scriptRepository,
                assetRepository,
                experimentVideoAssetRepository,
                landingVideoSlotRepository,
                reprocessPolicy);
    }

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

    @Test
    void shouldRejectWhenProfileDoesNotExist() {
        long missingId = 404L;
        given(profileRepository.findById(missingId)).willReturn(Optional.empty());

        assertThrows(VideoModuleException.class, () -> service.listJobsByProfile(missingId));
    }

    @Test
    void shouldCompleteRenderWithR2UrlAndSyncExperimentVideoSlot() {
        Experiment experiment = Experiment.builder().id(63L).build();
        LandingPage landingPage = LandingPage.builder().id(8L).experiment(experiment).build();
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .id(12L)
                .tenantId("default")
                .landingPage(landingPage)
                .build();
        SalesVideoJob job = SalesVideoJob.builder()
                .id(10108L)
                .profile(profile)
                .tenantId("default")
                .jobType(SalesVideoJobType.RENDER)
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .providerName("VEO")
                .status(SalesVideoStatus.VIDEO_PROCESSING)
                .build();
        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
                .id(44L)
                .experiment(experiment)
                .salesVideoProfile(profile)
                .salesVideoJob(job)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .status(ExperimentVideoStatus.GENERATING)
                .build();
        JobCompletionRequest request = new JobCompletionRequest();
        request.setAssetUrl("https://cdn.test/exp-63/video.mp4");
        request.setProviderJobId("veo-job-63");
        request.setMetadataJson("{\"provider\":\"VEO\"}");
        given(jobRepository.findById(10108L)).willReturn(Optional.of(job));
        given(assetRepository.findByUrlIn(List.of("https://cdn.test/exp-63/video.mp4"))).willReturn(List.of());
        given(assetRepository.save(any(Asset.class))).willAnswer(invocation -> {
            Asset saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        given(experimentVideoAssetRepository.findFirstBySalesVideoJobId(10108L)).willReturn(Optional.of(videoAsset));
        given(landingVideoSlotRepository.findByLandingPageIdAndSlotName(8L, "LANDING_HERO")).willReturn(Optional.empty());
        given(landingVideoSlotRepository.save(any(LandingVideoSlot.class))).willAnswer(invocation -> {
            LandingVideoSlot slot = invocation.getArgument(0);
            slot.setId(5L);
            return slot;
        });

        SalesVideoJobDto dto = service.complete(10108L, request);

        assertThat(dto.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
        assertThat(dto.getAssetUrl()).isEqualTo("https://cdn.test/exp-63/video.mp4");
        assertThat(job.getAsset().getStatus()).isEqualTo(AssetStatus.READY);
        assertThat(videoAsset.getStatus()).isEqualTo(ExperimentVideoStatus.READY);
        assertThat(videoAsset.getAssetUrl()).isEqualTo("https://cdn.test/exp-63/video.mp4");
        assertThat(videoAsset.getLandingVideoSlot().getId()).isEqualTo(5L);
        verify(experimentVideoAssetRepository).save(videoAsset);
    }
}

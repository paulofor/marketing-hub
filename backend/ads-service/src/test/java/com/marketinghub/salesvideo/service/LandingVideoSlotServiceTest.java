package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.CreateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotHistoryRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LandingVideoSlotServiceTest {

    @Mock
    private LandingVideoSlotRepository slotRepository;

    @Mock
    private LandingPageRepository landingPageRepository;

    @Mock
    private SalesVideoProfileRepository profileRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private LandingVideoSlotHistoryRepository historyRepository;

    @Mock
    private ExperimentVideoAssetRepository experimentVideoAssetRepository;

    private LandingVideoSlotService service;

    @BeforeEach
    void setUp() {
        service = new LandingVideoSlotService(slotRepository,
                landingPageRepository,
                profileRepository,
                assetRepository,
                historyRepository,
                experimentVideoAssetRepository);
    }

    @Test
    void shouldListSlotsForLanding() {
        long landingId = 7L;
        LandingPage landingPage = LandingPage.builder().id(landingId).build();
        given(landingPageRepository.findById(landingId)).willReturn(Optional.of(landingPage));
        TenantContextHolder.set(new TenantContext("tenant-test", "tester@local", false));
        SalesVideoProfile profile = SalesVideoProfile.builder().id(15L).tenantId("tenant-test").build();
        Asset videoAsset = Asset.builder().id(88L).url("https://cdn.test/video.mp4").build();
        LandingVideoSlot slot = LandingVideoSlot.builder()
                .id(3L)
                .landingPage(landingPage)
                .profile(profile)
                .slotName("hero")
                .asset(videoAsset)
                .autoplay(true)
                .muted(true)
                .loopVideo(false)
                .controlsEnabled(true)
                .lazyLoad(true)
                .build();
        given(slotRepository.findByLandingPageIdAndTenantId(landingId, "tenant-test")).willReturn(List.of(slot));

        try {
            List<LandingVideoSlotDto> result = service.list(landingId);

            assertThat(result).hasSize(1);
            LandingVideoSlotDto dto = result.get(0);
            assertThat(dto.getSlotName()).isEqualTo("hero");
            assertThat(dto.getAssetUrl()).contains("video.mp4");
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void shouldFailWhenLandingDoesNotExist() {
        long landingId = 999L;
        given(landingPageRepository.findById(landingId)).willReturn(Optional.empty());
        TenantContextHolder.set(new TenantContext("tenant-test", "tester@local", false));

        try {
            assertThrows(VideoModuleException.class, () -> service.list(landingId));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void shouldBlockPublicationWhenExperimentVideoAssetIsNotApproved() {
        long landingId = 7L;
        long profileId = 15L;
        long assetId = 88L;
        LandingPage landingPage = LandingPage.builder().id(landingId).build();
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .id(profileId)
                .tenantId("tenant-test")
                .humanReviewApprovedBy("reviewer@local")
                .humanReviewApprovedAt(Instant.now())
                .build();
        Asset videoAsset = Asset.builder().id(assetId).url("https://cdn.test/video.mp4").build();
        ExperimentVideoAsset experimentVideoAsset = ExperimentVideoAsset.builder()
                .id(99L)
                .experiment(Experiment.builder().id(39L).build())
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Hero PDE")
                .primaryMetric("click")
                .provider("LUMA")
                .model("ray-3.2")
                .status(ExperimentVideoStatus.READY)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .asset(videoAsset)
                .requiredForRelease(true)
                .build();
        CreateLandingVideoSlotRequest request = new CreateLandingVideoSlotRequest();
        request.setProfileId(profileId);
        request.setSlotName("hero");
        request.setAssetId(assetId);
        request.setPublishedBy("publisher@local");
        given(landingPageRepository.findById(landingId)).willReturn(Optional.of(landingPage));
        given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
        given(assetRepository.findById(assetId)).willReturn(Optional.of(videoAsset));
        given(experimentVideoAssetRepository.findByAssetId(assetId)).willReturn(List.of(experimentVideoAsset));
        TenantContextHolder.set(new TenantContext("tenant-test", "tester@local", false));

        try {
            VideoModuleException exception = assertThrows(VideoModuleException.class,
                    () -> service.create(landingId, request));

            assertThat(exception.getMessage()).contains("vídeo precisa estar pronto e aprovado");
        } finally {
            TenantContextHolder.clear();
        }
    }
}

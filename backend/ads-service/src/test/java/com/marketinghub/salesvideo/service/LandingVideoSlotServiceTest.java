package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.repository.LandingVideoSlotRepository;
import com.marketinghub.salesvideo.repository.LandingVideoSlotHistoryRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;

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

    private LandingVideoSlotService service;

    @BeforeEach
    void setUp() {
        service = new LandingVideoSlotService(slotRepository,
                landingPageRepository,
                profileRepository,
                assetRepository,
                historyRepository);
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
}

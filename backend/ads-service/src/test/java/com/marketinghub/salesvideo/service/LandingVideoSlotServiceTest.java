package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.repository.LandingVideoSlotRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    private LandingVideoSlotService service;

    @BeforeEach
    void setUp() {
        service = new LandingVideoSlotService(slotRepository,
                landingPageRepository,
                profileRepository,
                assetRepository);
    }

    @Test
    void shouldListSlotsForLanding() {
        long landingId = 7L;
        given(landingPageRepository.existsById(landingId)).willReturn(true);
        LandingPage landingPage = LandingPage.builder().id(landingId).build();
        SalesVideoProfile profile = SalesVideoProfile.builder().id(15L).build();
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
        given(slotRepository.findByLandingPageId(landingId)).willReturn(List.of(slot));

        List<LandingVideoSlotDto> result = service.list(landingId);

        assertThat(result).hasSize(1);
        LandingVideoSlotDto dto = result.get(0);
        assertThat(dto.getSlotName()).isEqualTo("hero");
        assertThat(dto.getAssetUrl()).contains("video.mp4");
    }

    @Test
    void shouldFailWhenLandingDoesNotExist() {
        long landingId = 999L;
        given(landingPageRepository.existsById(landingId)).willReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.list(landingId));
    }
}

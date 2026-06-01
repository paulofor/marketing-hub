package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
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

    private SalesVideoJobService service;

    @BeforeEach
    void setUp() {
        service = new SalesVideoJobService(jobRepository,
                eventRepository,
                profileRepository,
                scriptRepository,
                assetRepository,
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
}

package com.marketinghub.facebookadsworker.facebooktargeting;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.TargetingCandidateResolutionUpdate;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.TargetingOptionPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetingResolverServiceTest {
    @Mock
    private FacebookAdsService facebookAdsService;

    @Mock
    private TargetingBackendClient backendClient;

    private TargetingResolverService service;

    @BeforeEach
    void setUp() {
        TargetingResolverProperties properties = new TargetingResolverProperties();
        service = new TargetingResolverService(facebookAdsService, backendClient, properties);
    }

    @Test
    void resolveCandidateWithMatchReportsValidatedOptions() {
        FacebookAdsService.FacebookTargetingSearchResult result =
            new FacebookAdsService.FacebookTargetingSearchResult("600313", "Pilates", 1200000L, List.of("Interesses", "Fitness"));
        when(facebookAdsService.searchTargetingOptions(any())).thenReturn(List.of(result));

        TargetingResolutionRequest request = new TargetingResolutionRequest();
        request.setCandidates(List.of(new TargetingCandidatePayload(
            10L,
            "Pilates",
            TargetingCandidateType.INTEREST,
            null,
            null,
            null,
            null,
            null,
            null
        )));

        TargetingResolutionResponse response = service.resolve(UUID.randomUUID(), request);

        assertThat(response.candidates()).hasSize(1);
        TargetingResolutionResponse.CandidateResolutionSummary summary = response.candidates().get(0);
        assertThat(summary.status()).isEqualTo(TargetingCandidateStatus.VALIDATED);
        assertThat(summary.resolvedOptions()).isEqualTo(1);

        ArgumentCaptor<TargetingCandidateResolutionUpdate> captor = ArgumentCaptor.forClass(TargetingCandidateResolutionUpdate.class);
        verify(backendClient).reportResolution(eq(10L), captor.capture());
        TargetingCandidateResolutionUpdate update = captor.getValue();
        assertThat(update.status()).isEqualTo(TargetingCandidateStatus.VALIDATED);
        assertThat(update.options()).hasSize(1);
        TargetingOptionPayload option = update.options().get(0);
        assertThat(option.facebookId()).isEqualTo("600313");
        assertThat(option.name()).isEqualTo("Pilates");
        assertThat(option.matchScore()).isNotNull();
    }

    @Test
    void resolveCandidateWithoutMatchesReportsNoMatch() {
        when(facebookAdsService.searchTargetingOptions(any())).thenReturn(List.of());

        TargetingResolutionRequest request = new TargetingResolutionRequest();
        request.setLocale("pt_BR");
        request.setCountry("BR");
        request.setCandidates(List.of(new TargetingCandidatePayload(
            22L,
            "Termo inexistente",
            TargetingCandidateType.INTEREST,
            null,
            null,
            null,
            null,
            null,
            null
        )));

        TargetingResolutionResponse response = service.resolve(UUID.randomUUID(), request);

        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().get(0).status()).isEqualTo(TargetingCandidateStatus.NO_MATCH);

        ArgumentCaptor<TargetingCandidateResolutionUpdate> captor = ArgumentCaptor.forClass(TargetingCandidateResolutionUpdate.class);
        verify(backendClient).reportResolution(eq(22L), captor.capture());
        TargetingCandidateResolutionUpdate update = captor.getValue();
        assertThat(update.status()).isEqualTo(TargetingCandidateStatus.NO_MATCH);
        assertThat(update.options()).isEmpty();
        verify(facebookAdsService, atLeastOnce()).searchTargetingOptions(any());
    }
}

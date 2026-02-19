package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentDiagnosticsServiceTest {

    @Mock
    private ExperimentService experimentService;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private FacebookAdsAdSetRepository adSetRepository;
    @Mock
    private FacebookAdsAdRepository adRepository;

    @InjectMocks
    private ExperimentDiagnosticsService service;

    @Test
    void shouldNotFlagPendingWhenLegacyMetaIdIsStoredInIdField() {
        Long experimentId = 10L;

        Experiment experiment = new Experiment();
        experiment.setId(experimentId);
        experiment.setStatus(ExperimentStatus.FAILED);

        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("1202334455667788");
        campaign.setExternalId(null);
        campaign.setName("Campanha legado");

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(campaignRepository.findByExperimentId(experimentId)).thenReturn(List.of(campaign));
        when(adSetRepository.findByCampaignIdIn(List.of(campaign.getId()))).thenReturn(List.of());

        ExperimentDiagnosticsDto diagnostics = service.diagnose(experimentId);

        assertThat(diagnostics.artifacts()).isEmpty();
        assertThat(diagnostics.headline()).isEqualTo("Experimento está marcado como FAILED");
    }
}

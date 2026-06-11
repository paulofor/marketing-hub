package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Testa as regras de parada automática do funil de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelAutoStopServiceTest {

    @Mock
    private ExperimentFunnelDiagnosticService diagnosticService;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;

    @InjectMocks
    private ExperimentFunnelAutoStopService service;

    private Experiment experiment;

    @BeforeEach
    void setUp() {
        experiment = new Experiment();
        experiment.setId(99L);
        experiment.setStatus(ExperimentStatus.RUNNING);
    }

    /**
     * Garante parada quando o interesse no anúncio falha com confiança estatística.
     */
    @Test
    void stopsExperimentWhenAdInterestStatisticallyFails() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_FORM_LEAD,
                "Acesso ao formulário",
                1199,
                6,
                0.005,
                0.015,
                0.0108,
                List.of(new FunnelThresholdCheckDto(0.015, 200, 0.0025, false, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-interest");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfAdInterestStatisticallyLow(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.TARGET_AUDIENCE_LOW_INTEREST_STATISTICAL);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    @Test
    void stopsExperimentWhenThreePercentThresholdFails() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ENVIO_FORM,
                "Envio",
                120,
                0,
                0.0,
                0.10,
                0.025,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.025, true, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.RULE_OF_THREE_FAILED,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-1");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfFormSubmissionZeroConversions(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason()).isEqualTo(FacebookCampaignStopReason.FORM_ZERO_CONVERSION_RULE_OF_THREE);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    @Test
    void doesNothingWhenThresholdNotReached() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ENVIO_FORM,
                "Envio",
                20,
                0,
                0.0,
                0.10,
                0.15,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.15, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));

        boolean stopped = service.stopIfFormSubmissionZeroConversions(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    @Test
    void stopsExperimentWhenCampaignHasLowImpressionsAfterTwoDays() {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-impressions");
        campaign.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(experiment, 42L, campaign.getCreatedAt());

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason()).isEqualTo(FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    @Test
    void keepsExperimentRunningWhenLowImpressionsCampaignIsStillRecent() {
        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(
                experiment,
                42L,
                Instant.now().minus(2, ChronoUnit.HOURS));

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    @Test
    void keepsExperimentRunningWhenImpressionsReachMinimumAfterTwoDays() {
        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(
                experiment,
                100L,
                Instant.now().minus(49, ChronoUnit.HOURS));

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

}

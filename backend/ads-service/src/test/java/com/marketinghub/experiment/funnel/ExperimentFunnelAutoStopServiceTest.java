package com.marketinghub.experiment.funnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa as regras atuais de parada automática por política única de campanha. */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelAutoStopServiceTest {

  @Mock private ExperimentFunnelDiagnosticService diagnosticService;
  @Mock private FacebookAdsCampaignRepository campaignRepository;
  @Mock private ExperimentCampaignMetricRepository campaignMetricRepository;

  private ExperimentFunnelAutoStopService service;
  private Experiment experiment;

  /** Cria serviço real com dependências controladas por mock. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentFunnelAutoStopService(
            diagnosticService,
            new ExperimentFunnelStandbyService(campaignRepository),
            campaignMetricRepository);
    experiment = new Experiment();
    experiment.setId(99L);
    experiment.setStatus(ExperimentStatus.RUNNING);
  }

  /** Garante parada quando a campanha gasta R$ 25,00 sem resultado primário. */
  @Test
  void stopsCampaignWhenMinimumSpendHasNoPrimaryResult() {
    when(diagnosticService.diagnose(99L))
        .thenReturn(
            new ExperimentFunnelDiagnosticsResponseDto(
                List.of(
                    stage(
                        ExperimentFunnelStage.ENVIO_FORM,
                        80,
                        0,
                        FunnelDiagnosticStatus.INSUFFICIENT_DATA),
                    stage(
                        ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
                        0,
                        0,
                        FunnelDiagnosticStatus.NO_DATA),
                    stage(
                        ExperimentFunnelStage.COMPRA,
                        1,
                        0,
                        FunnelDiagnosticStatus.INSUFFICIENT_DATA)),
                null));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metricWithSpend("25.00")));
    FacebookAdsCampaign campaign = campaign("camp-zero-primary-result");
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    boolean stopped = service.stopIfNoPrimaryResultAfterMinimumSpend(experiment);

    assertThat(stopped).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.CAMPAIGN_ZERO_RESULT_AFTER_MINIMUM_SPEND);
    assertThat(campaign.getStopRequestedAt()).isNotNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  /**
   * Garante que a liquidação final invalida o experimento e corrige a causa mesmo após a Meta ter
   * sido reconciliada como pausa externa.
   */
  @Test
  void stopsUserStoppedExperimentWhenFinalMetricConfirmsMinimumSpend() {
    experiment.setStatus(ExperimentStatus.USER_STOPPED);
    when(diagnosticService.diagnose(99L))
        .thenReturn(
            new ExperimentFunnelDiagnosticsResponseDto(
                List.of(
                    stage(
                        ExperimentFunnelStage.ENVIO_FORM,
                        6,
                        0,
                        FunnelDiagnosticStatus.INSUFFICIENT_DATA),
                    stage(
                        ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
                        0,
                        0,
                        FunnelDiagnosticStatus.NO_DATA),
                    stage(ExperimentFunnelStage.COMPRA, 0, 0, FunnelDiagnosticStatus.NO_DATA)),
                null));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metricWithSpend("25.24")));
    FacebookAdsCampaign campaign = campaign("camp-paused-before-final-metric");
    Instant requestedAt = Instant.now().minus(2, ChronoUnit.MINUTES);
    Instant completedAt = Instant.now().minus(1, ChronoUnit.MINUTES);
    campaign.setStatus(FacebookAdStatus.PAUSED);
    campaign.setStopReason(FacebookCampaignStopReason.ADMIN_EXPERIMENT_PAUSED);
    campaign.setStopRequestedAt(requestedAt);
    campaign.setStopCompletedAt(completedAt);
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    boolean stopped = service.stopIfNoPrimaryResultAfterMinimumSpend(experiment);

    assertThat(stopped).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.CAMPAIGN_ZERO_RESULT_AFTER_MINIMUM_SPEND);
    assertThat(campaign.getStopRequestedAt()).isEqualTo(requestedAt);
    assertThat(campaign.getStopCompletedAt()).isEqualTo(completedAt);
    assertThat(campaign.getStopLastError()).isNull();
  }

  /** Garante que resultado primário existente impede a parada por gasto mínimo. */
  @Test
  void keepsCampaignRunningWhenPrimaryResultExists() {
    when(diagnosticService.diagnose(99L))
        .thenReturn(
            new ExperimentFunnelDiagnosticsResponseDto(
                List.of(
                    stage(
                        ExperimentFunnelStage.ENVIO_FORM,
                        80,
                        1,
                        FunnelDiagnosticStatus.HEALTHY_OR_INCONCLUSIVE)),
                null));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metricWithSpend("25.00")));

    boolean stopped = service.stopIfNoPrimaryResultAfterMinimumSpend(experiment);

    assertThat(stopped).isFalse();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
  }

  /** Garante parada quando qualquer etapa prioritária reprova estatisticamente. */
  @Test
  void stopsCampaignWhenAnyPrioritizedStageStatisticallyFails() {
    when(diagnosticService.diagnose(99L))
        .thenReturn(
            new ExperimentFunnelDiagnosticsResponseDto(
                List.of(
                    stage(
                        ExperimentFunnelStage.COMPRA,
                        120,
                        0,
                        FunnelDiagnosticStatus.STATISTICALLY_FAILED)),
                null));
    FacebookAdsCampaign campaign = campaign("camp-statistically-failed");
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    boolean stopped = service.stopIfAnyPrioritizedStageStatisticallyFailed(experiment);

    assertThat(stopped).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.CAMPAIGN_STATISTICALLY_FAILED_STAGE);
    assertThat(campaign.getStopRequestedAt()).isNotNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  /** Garante parada quando a campanha tem baixa entrega após dois dias. */
  @Test
  void stopsExperimentWhenCampaignHasLowImpressionsAfterTwoDays() {
    FacebookAdsCampaign campaign = campaign("camp-low-impressions");
    campaign.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    boolean stopped =
        service.stopIfLowImpressionsAfterRunningTime(experiment, 42L, campaign.getCreatedAt());

    assertThat(stopped).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME);
    assertThat(campaign.getStopRequestedAt()).isNotNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  /** Garante standby e pausa da campanha no primeiro envio válido do formulário. */
  @Test
  void putsRunningExperimentInStandbyAfterFirstValidFormSubmission() {
    FacebookAdsCampaign campaign = campaign("camp-first-submission");
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    boolean stopped = service.standbyOnFirstValidFormSubmission(experiment);

    assertThat(stopped).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.STANDBY);
    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.FIRST_FORM_SUBMISSION_STANDBY);
    assertThat(campaign.getStopRequestedAt()).isNotNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  /** Reabre pedido de parada quando a campanha voltou a ficar ativa após uma pausa antiga. */
  @Test
  void reopensStopRequestWhenCampaignIsActiveAfterCompletedStop() {
    FacebookAdsCampaign campaign = campaign("camp-reactivated-after-stop");
    Instant previousStopCompletedAt = Instant.now().minus(1, ChronoUnit.DAYS);
    campaign.setStatus(FacebookAdStatus.ACTIVE);
    campaign.setStopRequestedAt(previousStopCompletedAt.minus(1, ChronoUnit.MINUTES));
    campaign.setStopCompletedAt(previousStopCompletedAt);
    when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

    service.standbyOnFirstValidFormSubmission(experiment);

    assertThat(campaign.getStopReason())
        .isEqualTo(FacebookCampaignStopReason.FIRST_FORM_SUBMISSION_STANDBY);
    assertThat(campaign.getStopRequestedAt()).isAfter(previousStopCompletedAt);
    assertThat(campaign.getStopCompletedAt()).isNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  /** Cria métrica de campanha com gasto de mídia. */
  private ExperimentCampaignMetric metricWithSpend(String spend) {
    ExperimentCampaignMetric metric = new ExperimentCampaignMetric();
    metric.setExperiment(experiment);
    metric.setSpend(new BigDecimal(spend));
    return metric;
  }

  /** Cria campanha Facebook para validar pedido de parada. */
  private FacebookAdsCampaign campaign(String id) {
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId(id);
    return campaign;
  }

  /** Cria diagnóstico sintético para a política única de campanha. */
  private ExperimentFunnelStageDiagnosticDto stage(
      ExperimentFunnelStage stage, long attempts, long successes, FunnelDiagnosticStatus status) {
    return new ExperimentFunnelStageDiagnosticDto(
        stage,
        stage.getLabel(),
        attempts,
        successes,
        attempts > 0 ? (double) successes / attempts : null,
        0.03,
        null,
        List.of(),
        status,
        status == FunnelDiagnosticStatus.STATISTICALLY_FAILED
            ? FunnelDiagnosticReasonCode.RULE_OF_THREE_FAILED
            : FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
        "",
        false);
  }
}

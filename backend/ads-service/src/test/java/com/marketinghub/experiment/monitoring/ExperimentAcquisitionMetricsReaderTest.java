package com.marketinghub.experiment.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protege a identidade do motivo de parada enviado ao aprendizado entre agentes. */
class ExperimentAcquisitionMetricsReaderTest {
  /** Preserva a parada efetiva sem apresentar uma ação de início como diagnóstico histórico. */
  @Test
  void separatesStopExplanationFromUnrelatedStatusActions() {
    var campaign = new FacebookAdsCampaign();
    campaign.setStopReason(FacebookCampaignStopReason.CAMPAIGN_STATISTICALLY_FAILED_STAGE);
    campaign.setStopRequestedAt(Instant.parse("2026-10-22T01:58:00Z"));
    var experiment =
        Experiment.builder()
            .id(91005L)
            .platform(ExperimentPlatform.FACEBOOK)
            .campaignMetric(
                ExperimentCampaignMetric.builder()
                    .campaign(campaign)
                    .impressions(504L)
                    .clicks(2L)
                    .build())
            .build();
    var reader = new ExperimentAcquisitionMetricsReader();
    experiment.setLastStatusChangeAction("START");
    experiment.setLastStatusChangeReason("Início autorizado");
    var legacy = reader.read(experiment);
    assertThat(legacy.stopReason()).isEqualTo("CAMPAIGN_STATISTICALLY_FAILED_STAGE");
    assertThat(legacy.stopRequestedAt()).isEqualTo(campaign.getStopRequestedAt());
    assertThat(legacy.stopExplanation()).isNull();
    experiment.setLastStatusChangeAction("AUTOMATIC_STOP");
    experiment.setLastStatusChangeReason("Fixture: stage=CLICK; attempts=504; successes=2");
    assertThat(reader.read(experiment).stopExplanation())
        .isEqualTo(experiment.getLastStatusChangeReason());
    experiment.setCampaignMetric(null);
    assertThat(reader.read(experiment).stopReason()).isNull();
    assertThat(reader.read(experiment).stopExplanation()).isNull();
  }
}

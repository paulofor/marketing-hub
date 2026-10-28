package com.marketinghub.experiment.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jdbc.chatgptads.ChatgptAdsRepository;
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
    var repository = mock(ChatgptAdsRepository.class);
    var reader = new ExperimentAcquisitionMetricsReader(repository, new ObjectMapper());
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
    verifyNoInteractions(repository);
  }

  /**
   * Isola identidades e projeta somente gasto BRL completo, preservando ausência e moeda original.
   */
  @Test
  void readsNewChannelWithoutBorrowingMetaMetricsOrInventingExchangeRates() {
    var repository = mock(ChatgptAdsRepository.class);
    var reader = new ExperimentAcquisitionMetricsReader(repository, new ObjectMapper());
    for (long id : java.util.List.of(981L, 1981L)) {
      var experiment =
          Experiment.builder()
              .id(id)
              .platform(ExperimentPlatform.CHATGPT_ADS)
              .campaignMetric(
                  ExperimentCampaignMetric.builder().spend(new java.math.BigDecimal("999")).build())
              .build();
      when(repository.list(id)).thenReturn(java.util.List.of(snapshot(id, "BRL", "4.50")));
      var result = reader.read(experiment);
      assertThat(result.spendBrl()).isEqualByComparingTo("4.50");
      assertThat(result.finalSyncedAt()).isEqualTo(Instant.parse("2026-10-28T02:00:10Z"));
      assertThat(result.stopReason()).isEqualTo("MEDIA_SPEND_LIMIT_REACHED");
      when(repository.list(id)).thenReturn(java.util.List.of(snapshot(id, "USD", "4.50")));
      assertThat(reader.read(experiment).spendBrl()).isNull();
      assertThat(reader.read(experiment).lastError()).contains("conversão para BRL");
      when(repository.list(id)).thenReturn(java.util.List.of(snapshot(id, "BRL", "null")));
      assertThat(reader.read(experiment).spendBrl()).isNull();
      when(repository.list(id)).thenReturn(java.util.List.of());
      assertThat(reader.read(experiment).campaignLinked()).isFalse();
    }
  }

  /** Monta recibo sintético com cobertura posterior à pausa e sem qualquer credencial. */
  private ChatgptAdsRepository.Campaign snapshot(long id, String currency, String spend) {
    return new ChatgptAdsRepository.Campaign(
        id,
        id,
        "{}",
        "fixture",
        "{}",
        "PAUSED",
        "{\"campaignId\":\"camp_fixture\",\"stopReason\":\"MEDIA_SPEND_LIMIT_REACHED\",\"stopCompletedAt\":\"2026-10-28T01:15:00Z\"}",
        "{\"currency\":\""
            + currency
            + "\",\"spend\":"
            + spend
            + ",\"impressions\":100,\"clicks\":3,\"coverageEnd\":1793152800,\"observedAt\":\"2026-10-28T02:00:10Z\",\"finalSyncedAt\":\"2026-10-28T02:00:10Z\"}",
        Instant.now());
  }
}

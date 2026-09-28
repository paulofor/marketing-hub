package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.service.FacebookCampaignOperationalStatusService.DeliveryState;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida a separação entre configuração Meta e capacidade real de entrega. */
class FacebookCampaignOperationalStatusServiceTest {

  /** Não trata ACTIVE como entrega quando a janela oficial já terminou. */
  @Test
  void reportsEndedWindowEvenWhenMetaConfigurationIsActive() {
    FacebookAdsCampaignRepository repository = mock(FacebookAdsCampaignRepository.class);
    FacebookAdsCampaign campaign = campaign("cmp-91");
    campaign.setStatus(FacebookAdStatus.ACTIVE);
    campaign.setMetaConfiguredStatus("ACTIVE");
    campaign.setMetaEffectiveStatus("ACTIVE");
    campaign.setMetaStopTime(Instant.parse("2026-09-27T02:59:59Z"));
    campaign.setStatusLastSyncedAt(Instant.parse("2026-09-28T11:59:00Z"));
    FacebookAdsAdSet adSet = new FacebookAdsAdSet();
    adSet.setBudgetRemainingMinor(47L);
    campaign.getAdSets().add(adSet);
    when(repository.findDetailedByExperimentId(91L)).thenReturn(List.of(campaign));
    var service = service(repository);

    var summary = service.summarize(91L);

    assertThat(summary.configuredStatus()).isEqualTo("ACTIVE");
    assertThat(summary.deliveryState()).isEqualTo(DeliveryState.WINDOW_ENDED);
    assertThat(summary.deliveringNow()).isFalse();
    assertThat(summary.budgetRemainingMinor()).isEqualTo(47L);
  }

  /** Mantém entrega desconhecida quando a campanha está apenas apta e com status recente. */
  @Test
  void doesNotInventDeliveryForEligibleActiveCampaign() {
    FacebookAdsCampaignRepository repository = mock(FacebookAdsCampaignRepository.class);
    FacebookAdsCampaign campaign = campaign("cmp-current");
    campaign.setStatus(FacebookAdStatus.ACTIVE);
    campaign.setMetaConfiguredStatus("ACTIVE");
    campaign.setMetaEffectiveStatus("ACTIVE");
    campaign.setMetaStartTime(Instant.parse("2026-09-27T03:00:00Z"));
    campaign.setMetaStopTime(Instant.parse("2026-09-30T02:59:59Z"));
    campaign.setStatusLastSyncedAt(Instant.parse("2026-09-28T11:59:00Z"));
    when(repository.findDetailedByExperimentId(93L)).thenReturn(List.of(campaign));
    var service = service(repository);

    var summary = service.summarize(93L);

    assertThat(summary.deliveryState()).isEqualTo(DeliveryState.ELIGIBLE_NOT_CONFIRMED);
    assertThat(summary.deliveringNow()).isNull();
  }

  /** Cria o serviço com relógio fixo na rodada investigada. */
  private FacebookCampaignOperationalStatusService service(
      FacebookAdsCampaignRepository repository) {
    return new FacebookCampaignOperationalStatusService(
        repository, Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC));
  }

  /** Cria uma campanha vigente para o experimento esperado pelo repositório simulado. */
  private FacebookAdsCampaign campaign(String id) {
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId(id);
    campaign.setCreatedAt(Instant.parse("2026-09-20T12:00:00Z"));
    return campaign;
  }
}

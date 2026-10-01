package com.marketinghub.facebookadsworker.facebookcampaign;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protege a cadência local e a coleta definitiva de métricas tardias sem reativar campanhas. */
class FacebookCampaignMetricSettlementTest {
  /** Ativas e primeiro retorno terminal não aguardam a cadência de consolidação. */
  @Test
  void activeAndFirstSettlementRemainImmediatelyEligible() {
    var now = Instant.parse("2026-10-01T00:00:00Z");
    assertTrue(FacebookCampaignMetricsService.isSettlementRefreshDue(
        new FacebookCampaignMetricsService.CampaignMetricsSyncTarget("campaign", 93L, null, now), now));
    assertTrue(FacebookCampaignMetricsService.isSettlementRefreshDue(
        new FacebookCampaignMetricsService.CampaignMetricsSyncTarget("campaign", 93L, null, null, null, now.plusSeconds(172800)), now));
  }

  /** Consultas repetidas aguardam seis horas e o prazo final prevalece sobre essa espera. */
  @Test
  void settlementWaitsSixHoursButAlwaysCollectsAtDeadline() {
    var now = Instant.parse("2026-10-01T00:00:00Z");
    var target = new FacebookCampaignMetricsService.CampaignMetricsSyncTarget(
        "campaign", 93L, null, now, null, now.plusSeconds(172800));
    assertFalse(FacebookCampaignMetricsService.isSettlementRefreshDue(target, now.plusSeconds(21599)));
    assertTrue(FacebookCampaignMetricsService.isSettlementRefreshDue(target, now.plusSeconds(21600)));
    var finalTarget = new FacebookCampaignMetricsService.CampaignMetricsSyncTarget(
        "campaign", 93L, null, now.plusSeconds(170000), null, now.plusSeconds(172800));
    assertTrue(FacebookCampaignMetricsService.isSettlementRefreshDue(finalTarget, now.plusSeconds(172800)));
  }
}

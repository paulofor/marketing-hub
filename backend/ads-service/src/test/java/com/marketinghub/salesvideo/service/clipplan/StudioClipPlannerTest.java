package com.marketinghub.salesvideo.service.clipplan;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.salesvideo.SalesVideoProviderModel;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger prévia comercial de duração, preço e homologação sem geração. */
class StudioClipPlannerTest {
  private static final Instant NOW = Instant.parse("2026-11-05T15:00:00Z");

  /** Mostra quatro tomadas e o custo parcial de 32 segundos sem autorizar produção. */
  @Test
  void previews32SecondsWithExactCostAndNarrative() {
    var plan =
        StudioClipPlanner.preview(
            active(), new StudioClipPlanRequest("RUNWAY", 32, "720p", false, 0, 0), NOW);
    assertThat(plan.scenes())
        .extracting(StudioClipPlanResponse.Scene::billedSeconds)
        .containsExactly(10, 10, 10, 2);
    assertThat(plan.clipsCostUsd()).isEqualByComparingTo("3.84");
    assertThat(plan.productionAllowed()).isFalse();
    assertThat(plan.scenes().get(2).narrativeRole()).contains("demonstração real");
    assertThat(plan.blockers()).anyMatch(value -> value.contains("Agentes"));
  }

  /** Não transforma cadastro público ou preço ausente em integração ativa ou custo zero. */
  @Test
  void draftAndStalePriceRemainBlocked() {
    var plan =
        StudioClipPlanner.preview(
            null, new StudioClipPlanRequest("RUNWAY_KLING_4", 15, "720p", true, 0, 0), NOW);
    assertThat(plan.productionAllowed()).isFalse();
    assertThat(plan.blockers()).anyMatch(value -> value.contains("homologação"));
    var stale =
        StudioClipPlanner.preview(
            active(),
            new StudioClipPlanRequest("RUNWAY", 32, "720p", false, 0, 0),
            Instant.parse("2026-11-12T00:00:00Z"));
    assertThat(stale.clipsCostUsd()).isNull();
    assertThat(stale.scenes()).allSatisfy(scene -> assertThat(scene.maximumCostUsd()).isNull());
  }

  /** Confere outro modelo sem reutilizar duração, preço ou contexto de um produto anterior. */
  @Test
  void secondContextVeoHasExplicitAssemblyTrim() {
    var plan =
        StudioClipPlanner.preview(
            active(),
            new StudioClipPlanRequest("RUNWAY_VEO_3_1_FAST", 15, "720p", true, 0, 0),
            NOW);
    assertThat(plan.finalSeconds()).isEqualTo(15);
    assertThat(plan.generatedSeconds()).isEqualTo(16);
    assertThat(plan.trimSeconds()).isEqualTo(1);
    assertThat(plan.clipsCostUsd()).isEqualByComparingTo("2.40");
  }

  /** Monta cadastro sintético para a simulação, sem promovê-lo em ambiente publicado. */
  static SalesVideoProviderModel active() {
    var model = new SalesVideoProviderModel();
    model.setLifecycleStatus("ACTIVE");
    model.setAdapterVerified(true);
    model.setPricingVerified(true);
    model.setQualityGateVerified(true);
    model.setCommercialLicenseVerified(true);
    return model;
  }
}

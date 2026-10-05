package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: distinguir versões explícitas e legadas sem inferir compatibilidade. */
class CommercialAssumptionVersionCompatibilityTest {
  /** Uma versão explícita não depende de precisão temporal perdida pelo banco antigo. */
  @Test
  void usesExplicitVersionInsteadOfTimestampPrecision() {
    var current =
        new CommercialPlanVersionDto(
            401L, 701L, 3, "{}", "QA", "Contexto", Instant.parse("2026-10-05T12:00:00.550Z"));
    var source = new ExperimentStrategistExecution();
    source.setCreatedAt(Instant.parse("2026-10-05T12:00:00Z"));
    source.setEvidenceSnapshot("{\"commercialPlan\":{\"version\":3}}");
    assertThat(CommercialAssumptionVersionCompatibility.matches(source, current)).isTrue();
    source.setEvidenceSnapshot("{\"commercialPlan\":{\"version\":2}}");
    assertThat(CommercialAssumptionVersionCompatibility.matches(source, current)).isFalse();
    source.setEvidenceSnapshot("{}");
    assertThat(CommercialAssumptionVersionCompatibility.matches(source, current)).isFalse();
    source.setCreatedAt(Instant.parse("2026-10-05T12:00:01Z"));
    assertThat(CommercialAssumptionVersionCompatibility.matches(source, current)).isTrue();
  }

  /** Snapshot inválido é bloqueio e não motivo para repetir a inferência paga. */
  @Test
  void rejectsMalformedOrUntypedSnapshots() {
    var current =
        new CommercialPlanVersionDto(402L, 702L, 4, "{}", "QA", "Contexto", Instant.now());
    var source = new ExperimentStrategistExecution();
    source.setId(802L);
    source.setEvidenceSnapshot("invalid-json");
    assertThatThrownBy(() -> CommercialAssumptionVersionCompatibility.matches(source, current))
        .hasMessageContaining("Snapshot da proposta inválido");
    source.setEvidenceSnapshot("{\"commercialPlan\":{\"version\":\"4\"}}");
    assertThatThrownBy(() -> CommercialAssumptionVersionCompatibility.matches(source, current))
        .hasMessageContaining("Versão da proposta inválida");
  }
}

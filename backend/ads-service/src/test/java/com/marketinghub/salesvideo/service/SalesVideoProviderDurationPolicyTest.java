package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** Responsabilidade: preservar limites de clipe coerentes com a seleção explícita do Estúdio. */
class SalesVideoProviderDurationPolicyTest {
  /** Evita que a seleção de Gen-4.5 herde duração de outra rota citada no texto. */
  @ParameterizedTest
  @CsvSource({
    "Provider escolhido: Gen-4.5 (RUNWAY).,10",
    "Gen-4.5 (RUNWAY_GEN_4_5).,10",
    "Gen-4.5 (runway). Não usar Luma ou SEEDANCE_2_5 neste plano.,10",
    "(RUNWAY_GEN_4_TURBO),10",
    "(RUNWAY_SEEDANCE_2_5),15",
    "(RUNWAY_SEEDANCE_2),15",
    "(RUNWAY_VEO_3_1_FAST),8",
    "(RUNWAY_PRODUCT_UGC),15",
    "(RUNWAY) no histórico; escolha premium (RUNWAY_PRODUCT_UGC),15",
    "(RUNWAY_HAILUO_3),10",
    "LUMA_RAY_3_2 como principal no plano legado.,15",
    "Plano legado RUNWAY_SEEDANCE_2_5,15",
    "Plano legado RUNWAY_VEO_3_1,8",
    "Texto sem seleção explícita,10"
  })
  void shouldPreserveSelectedProviderLimit(String plan, int expectedSeconds) {
    assertThat(SalesVideoProviderDurationPolicy.maxClipSecondsForPlan(plan))
        .isEqualTo(expectedSeconds);
  }

  /** Usa o limite genérico de dez segundos quando o plano não informa uma seleção. */
  @ParameterizedTest
  @NullAndEmptySource
  void shouldUseConservativeClipLimitWhenPlanIsMissing(String plan) {
    assertThat(SalesVideoProviderDurationPolicy.maxClipSecondsForPlan(plan)).isEqualTo(10);
  }
}

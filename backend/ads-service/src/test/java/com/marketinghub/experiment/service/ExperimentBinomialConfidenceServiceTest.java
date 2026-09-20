package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Testa o intervalo exato que sustenta as decisões de amostra do experimento. */
class ExperimentBinomialConfidenceServiceTest {

  private final ExperimentBinomialConfidenceService service =
      new ExperimentBinomialConfidenceService();

  /** Reproduz os intervalos comerciais de cinco em cem e vinte e cinco em quinhentos. */
  @Test
  void calculatesExactIntervalsForInitialAndPrecisionSamples() {
    var initial = service.exact95(100, 5);
    var precision = service.exact95(500, 25);

    assertThat(initial.lower() * 100.0d).isCloseTo(1.64d, within(0.005d));
    assertThat(initial.upper() * 100.0d).isCloseTo(11.28d, within(0.005d));
    assertThat(precision.lower() * 100.0d).isCloseTo(3.26d, within(0.005d));
    assertThat(precision.upper() * 100.0d).isCloseTo(7.29d, within(0.005d));
  }

  /** Mantém os limites exatos dentro de zero e um nas duas bordas da distribuição. */
  @Test
  void calculatesExactBoundaryIntervals() {
    var zeroPurchases = service.exact95(100, 0);
    var allPurchases = service.exact95(100, 100);

    assertThat(zeroPurchases.lower()).isZero();
    assertThat(zeroPurchases.upper() * 100.0d).isCloseTo(3.62d, within(0.005d));
    assertThat(allPurchases.lower() * 100.0d).isCloseTo(96.38d, within(0.005d));
    assertThat(allPurchases.upper()).isEqualTo(1.0d);
  }

  /** Rejeita contagens impossíveis antes de iniciar a inversão estatística. */
  @Test
  void rejectsInvalidCounts() {
    assertThatThrownBy(() -> service.exact95(4, 5)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.exact95(0, 0)).isInstanceOf(IllegalArgumentException.class);
  }
}

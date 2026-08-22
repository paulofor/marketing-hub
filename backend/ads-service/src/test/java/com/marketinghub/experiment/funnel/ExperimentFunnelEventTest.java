package com.marketinghub.experiment.funnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Testa a preservação do instante de origem nos eventos brutos do funil. */
class ExperimentFunnelEventTest {

  /**
   * Garante que o instante informado pela origem não seja substituído no momento da persistência.
   */
  @Test
  void initializeOccurredAtPreservesSourceInstant() {
    Instant sourceInstant = Instant.parse("2026-08-22T12:06:45.986Z");
    ExperimentFunnelEvent event = ExperimentFunnelEvent.builder().occurredAt(sourceInstant).build();

    event.initializeOccurredAt();

    assertEquals(sourceInstant, event.getOccurredAt());
  }

  /** Garante que eventos sem horário explícito recebam um instante antes de serem persistidos. */
  @Test
  void initializeOccurredAtFillsMissingInstant() {
    ExperimentFunnelEvent event = ExperimentFunnelEvent.builder().build();

    event.initializeOccurredAt();

    assertNotNull(event.getOccurredAt());
  }
}

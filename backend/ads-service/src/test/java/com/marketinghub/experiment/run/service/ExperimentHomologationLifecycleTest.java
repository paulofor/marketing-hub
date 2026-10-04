package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Responsabilidade: conferir encerramento independente de produto, tipo, campanha ou Plutus. */
class ExperimentHomologationLifecycleTest {
  private final LocalDate today = LocalDate.of(2026, 10, 4);

  /** Nenhum estado comercial terminal pode pedir outra homologação, mesmo com data futura. */
  @ParameterizedTest
  @EnumSource(
      value = ExperimentStatus.class,
      names = {"VALIDATED", "INVALIDATED", "INCONCLUSIVE", "FINISHED", "FAILED"})
  void refusesTerminalStates(ExperimentStatus status) {
    var experiment = new Experiment();
    experiment.setId(97102L);
    experiment.setStatus(status);
    experiment.setEndDate(today.plusDays(3));
    assertThat(ExperimentHomologationLifecycle.blockReason(experiment, today))
        .contains("#97102", status.name(), "não renove Plutus", "novo ciclo/experimento");
  }

  /** Estados retomáveis continuam elegíveis até o último dia, sem dispensar outros gates. */
  @ParameterizedTest
  @EnumSource(
      value = ExperimentStatus.class,
      names = {"PLANNED", "RUNNING", "PAUSED", "STANDBY", "USER_STOPPED"})
  void preservesValidWindowAndBlocksExpiredWindow(ExperimentStatus status) {
    var experiment = new Experiment();
    experiment.setId(97103L);
    experiment.setStatus(status);
    assertThat(ExperimentHomologationLifecycle.blockReason(experiment, today)).isNull();
    experiment.setEndDate(today);
    assertThat(ExperimentHomologationLifecycle.blockReason(experiment, today)).isNull();
    experiment.setEndDate(today.minusDays(1));
    assertThat(ExperimentHomologationLifecycle.blockReason(experiment, today))
        .contains("2026-10-03", "janela encerrada", "não autoriza gasto");
    assertThat(experiment.getStatus()).isEqualTo(status);
  }

  /** O caso observado preserva a janela original e não interpreta parecer novo como retomada. */
  @Test
  void explainsOriginalClosedReference() {
    var experiment = new Experiment();
    experiment.setId(88L);
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    experiment.setEndDate(LocalDate.of(2026, 9, 29));
    assertThat(ExperimentHomologationLifecycle.blockReason(experiment, today))
        .contains("#88", "INVALIDATED", "2026-09-29", "Preserve resultados, custos e provas");
  }
}

package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: recuperar a prova da publicação encerrada sem reabrir campanha ou autorizar
 * gasto.
 */
class LearningCyclePublicationRecoveryTest {
  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final LearningCycleEvidence evidence =
      new LearningCycleEvidence(null, runs, null, null, null);
  private final Instant authorized = Instant.parse("2026-10-20T10:50:48Z");
  private final Instant published = Instant.parse("2026-10-21T14:39:12Z");

  /** Reproduz Capella, outro produto e a publicação ainda ativa, sem alterar o estado comercial. */
  @ParameterizedTest
  @CsvSource({"105,INVALIDATED", "92105,FINISHED", "93105,USER_STOPPED", "94105,RUNNING"})
  void recognizesPublicationAfterCampaignStopped(long experimentId, ExperimentStatus status) {
    var experiment = experiment(experimentId, status);
    var cycle = cycle(experimentId);
    var run = run(experiment);
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            experimentId, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    assertThatCode(() -> evidence.publication(cycle, experiment, authorized))
        .doesNotThrowAnyException();
    assertThat(experiment.getStatus()).isEqualTo(status);
    verifyNoMoreInteractionsExceptRead(experimentId);
  }

  /** Mantém os bloqueios de preflight, autorização, janela e orçamento no histórico. */
  @Test
  void historicalPublicationStillRequiresOriginalEvidence() {
    var experiment = experiment(105L, ExperimentStatus.INVALIDATED);
    var cycle = cycle(105L);
    var run = run(experiment);
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(105L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    run.setPreflightCompletedAt(null);
    assertThatThrownBy(() -> evidence.publication(cycle, experiment, authorized))
        .hasMessageContaining("preflight");
    run.setPreflightCompletedAt(published.minusSeconds(60));
    assertThatThrownBy(() -> evidence.publication(cycle, experiment, published.plusSeconds(1)))
        .hasMessageContaining("autorização");
    cycle.setWindowEnd(published.minusSeconds(1));
    assertThatThrownBy(() -> evidence.publication(cycle, experiment, authorized))
        .hasMessageContaining("janela");
    cycle.setWindowEnd(published.plusSeconds(60));
    experiment.setMediaSpendLimit(BigDecimal.valueOf(200));
    assertThatThrownBy(() -> evidence.publication(cycle, experiment, authorized))
        .hasMessageContaining("teto");
  }

  /** Um estado encerrado isolado não comprova que houve publicação. */
  @Test
  void terminalStatusWithoutPublicationCannotAdvance() {
    var experiment = experiment(105L, ExperimentStatus.INVALIDATED);
    assertThatThrownBy(() -> evidence.publication(cycle(105L), experiment, authorized))
        .hasMessageContaining("preflight");
  }

  /** Confere que a recuperação fez somente a leitura da prova operacional. */
  private void verifyNoMoreInteractionsExceptRead(long id) {
    verify(runs).findTopByExperimentIdAndModeOrderByRunNumberDesc(id, ExperimentRunMode.PRODUCTION);
    verifyNoMoreInteractions(runs);
  }

  /** Cria um experimento segregado com orçamento autorizado. */
  private Experiment experiment(long id, ExperimentStatus status) {
    var experiment = new Experiment();
    experiment.setId(id);
    experiment.setStatus(status);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setMediaSpendLimit(BigDecimal.valueOf(100));
    return experiment;
  }

  /** Conserva a janela histórica do ciclo, mesmo quando a consulta ocorre depois dela. */
  private LearningSalesCycle cycle(long experimentId) {
    var cycle = new LearningSalesCycle();
    cycle.setExperimentId(experimentId);
    cycle.setBudgetLimitBrl(BigDecimal.valueOf(100));
    cycle.setWindowStart(authorized.minusSeconds(60));
    cycle.setWindowEnd(published.plusSeconds(60));
    return cycle;
  }

  /** Reproduz um run produtivo homologado e encerrado com exposição real registrada. */
  private ExperimentRun run(Experiment experiment) {
    var run = new ExperimentRun();
    run.setExperiment(experiment);
    run.setMode(ExperimentRunMode.PRODUCTION);
    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setPublishedAt(published);
    run.setPreflightCompletedAt(published.minusSeconds(60));
    run.setEndedAt(published.plusSeconds(600));
    return run;
  }
}

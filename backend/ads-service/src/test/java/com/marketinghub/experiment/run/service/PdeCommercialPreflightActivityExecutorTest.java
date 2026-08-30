package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a criação e continuidade idempotente do preflight pelo processo. */
class PdeCommercialPreflightActivityExecutorTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final BackendExperimentRunService runService = mock(BackendExperimentRunService.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final PdeCommercialPreflightActivityExecutor executor =
      new PdeCommercialPreflightActivityExecutor(experiments, runs, runService, predecessors);

  /** Executa o run em rascunho e orienta o registro das evidências funcionais pendentes. */
  @Test
  void runsDraftPreflightAndKeepsSingleRun() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product);
    ExperimentRun run = run(experiment, ExperimentRunStatus.DRAFT);
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(predecessors.readiness(any(), any(), any()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Revisões concluídas."));
    doAnswer(
            invocation -> {
              run.setStatus(ExperimentRunStatus.PREFLIGHT_PENDING);
              return null;
            })
        .when(runService)
        .runPreflight(12L);

    BackendProductProcessActivityExecutionResult result =
        executor.execute(process(), activity(), product, "experiment:89");

    verify(runService).runPreflight(12L);
    verify(runService).synchronizePreflightActivity(12L);
    assertThat(result.operationalState()).isEqualTo("PENDING");
    assertThat(result.objectiveAchieved()).isFalse();
    assertThat(result.message()).contains("evidências funcionais");
  }

  /** Desabilita a reexecução enquanto o run aguarda evidências para não apagá-las. */
  @Test
  void exposesWorkspaceWithoutRerunningPendingPreflight() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product);
    ExperimentRun run = run(experiment, ExperimentRunStatus.PREFLIGHT_PENDING);
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(predecessors.readiness(any(), any(), any()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Revisões concluídas."));

    BackendProductProcessActivityReadiness readiness =
        executor.readiness(process(), activity(), product, "experiment:89");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.workspaceCode()).isEqualTo("EXPERIMENT_PREFLIGHT");
    assertThat(readiness.workspaceReferenceId()).isEqualTo(89L);
    assertThat(readiness.reason()).contains("evidências funcionais");
  }

  /** Reconcilia um run concluído anteriormente sem reexecutar nem apagar seus gates. */
  @Test
  void reconcilesPreviouslyCompletedRun() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product);
    ExperimentRun run = run(experiment, ExperimentRunStatus.READY_TO_PUBLISH);
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(predecessors.readiness(any(), any(), any()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Revisões concluídas."));

    BackendProductProcessActivityExecutionResult result =
        executor.execute(process(), activity(), product, "experiment:89");

    verify(runService, never()).runPreflight(12L);
    verify(runService).synchronizePreflightActivity(12L);
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
  }

  /** Monta o experimento operacional do produto. */
  private Experiment experiment(Product product) {
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(product);
    return experiment;
  }

  /** Monta o run mais recente do experimento. */
  private ExperimentRun run(Experiment experiment, ExperimentRunStatus status) {
    return ExperimentRun.builder()
        .id(12L)
        .experiment(experiment)
        .runNumber(1)
        .mode(ExperimentRunMode.PRODUCTION)
        .status(status)
        .build();
  }

  /** Monta o processo publicado reconhecido pelo executor. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(56L);
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta a atividade de preflight reconhecida pelo executor. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(589L);
    activity.setActivityId("preflight");
    activity.setOwnerName("Backend");
    return activity;
  }
}

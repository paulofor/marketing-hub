package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.experiment.CreativeGenerationStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a ponte entre tarefas da Têmis e a fila de criativos. */
class TemisCreativeTaskOrchestrationServiceTest {

  /** Enfileira a tarefa real no experimento e a inicia sem duplicar alternativas. */
  @Test
  void reconcilesPendingTemisTaskIntoCreativeQueue() {
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTask task = task("PENDING", "experiment:88");
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    when(tasks.findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
            "meta-ad-approver", "WORK", List.of("PENDING", "IN_PROGRESS")))
        .thenReturn(List.of(task));
    when(experiments.findById(88L)).thenReturn(Optional.of(experiment));
    Instant now = Instant.parse("2026-08-12T17:00:00Z");
    TemisCreativeTaskOrchestrationService service =
        new TemisCreativeTaskOrchestrationService(
            tasks, experiments, Clock.fixed(now, ZoneOffset.UTC));

    service.reconcilePendingTasks();
    service.reconcilePendingTasks();

    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(experiment.getCreativesToGenerate()).isEqualTo(1);
    assertThat(experiment.getCreativeGenerationStatus())
        .isEqualTo(CreativeGenerationStatus.REQUESTED);
    assertThat(experiment.getCreativeGenerationRequestedAt()).isEqualTo(now);
  }

  /** Reenfileira uma tarefa retomada depois de falha sem duplicar trabalho ainda ativo. */
  @Test
  void retriesResumedTemisTaskAfterCreativeGenerationFailure() {
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTask task = task("IN_PROGRESS", "experiment:88");
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    experiment.setCreativesToGenerate(0);
    experiment.setCreativeGenerationStatus(CreativeGenerationStatus.FAILED);
    when(tasks.findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
            "meta-ad-approver", "WORK", List.of("PENDING", "IN_PROGRESS")))
        .thenReturn(List.of(task));
    when(experiments.findById(88L)).thenReturn(Optional.of(experiment));
    TemisCreativeTaskOrchestrationService service =
        new TemisCreativeTaskOrchestrationService(
            tasks, experiments, Clock.fixed(Instant.parse("2026-08-12T18:40:00Z"), ZoneOffset.UTC));

    service.reconcilePendingTasks();

    assertThat(experiment.getCreativesToGenerate()).isEqualTo(1);
    assertThat(experiment.getCreativeGenerationStatus())
        .isEqualTo(CreativeGenerationStatus.REQUESTED);
  }

  /** Conclui a tarefa somente depois do callback de aprovação independente. */
  @Test
  void completesTaskAfterCreativeMaterializationCallback() {
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    AgentTask task = task("IN_PROGRESS", "experiment:88");
    when(tasks.findTopBySourceReferenceOrderByUpdatedAtDescIdDesc("experiment:88"))
        .thenReturn(Optional.of(task));
    TemisCreativeTaskOrchestrationService service =
        new TemisCreativeTaskOrchestrationService(
            tasks,
            mock(ExperimentRepository.class),
            Clock.fixed(Instant.parse("2026-08-12T17:05:00Z"), ZoneOffset.UTC));

    service.completeForExperiment(88L);

    assertThat(task.getStatus()).isEqualTo("COMPLETED");
  }

  /** Cria tarefa mínima com identidade técnica da Têmis. */
  private AgentTask task(String status, String reference) {
    Agent temis = new Agent();
    temis.setAgentKey("meta-ad-approver");
    AgentTask task = new AgentTask();
    task.setAssignedAgent(temis);
    task.setTaskKind("WORK");
    task.setStatus(status);
    task.setSourceReference(reference);
    return task;
  }
}

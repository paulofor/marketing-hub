package com.marketinghub.experiment.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.CreativeGenerationStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: ligar tarefas criativas da Têmis à fila canônica de geração de anúncios. */
@Service
public class TemisCreativeTaskOrchestrationService {
  private static final String TEMIS_AGENT_KEY = "meta-ad-approver";
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:(\\d+)$");
  private final AgentTaskRepository taskRepository;
  private final ExperimentRepository experimentRepository;
  private final Clock clock;

  /** Configura a orquestração com as fontes de verdade de tarefas e experimentos. */
  @Autowired
  public TemisCreativeTaskOrchestrationService(
      AgentTaskRepository taskRepository, ExperimentRepository experimentRepository) {
    this(taskRepository, experimentRepository, Clock.systemUTC());
  }

  /** Permite validar transições temporais de forma determinística. */
  TemisCreativeTaskOrchestrationService(
      AgentTaskRepository taskRepository, ExperimentRepository experimentRepository, Clock clock) {
    this.taskRepository = taskRepository;
    this.experimentRepository = experimentRepository;
    this.clock = clock;
  }

  /** Converte tarefas pendentes da Têmis em solicitações idempotentes na fila do AI Worker. */
  @Transactional
  public void reconcilePendingTasks() {
    List<AgentTask> tasks =
        taskRepository.findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
            TEMIS_AGENT_KEY, "WORK", List.of("PENDING", "IN_PROGRESS"));
    for (AgentTask task : tasks) {
      Long experimentId = experimentId(task.getSourceReference());
      if (experimentId == null) {
        continue;
      }
      Experiment experiment = experimentRepository.findById(experimentId).orElse(null);
      if (experiment == null) {
        block(task);
        continue;
      }
      if (shouldRequestCreative(task, experiment)) {
        requestOneCreative(experiment);
        task.setStatus("IN_PROGRESS");
        task.setUpdatedAt(Instant.now(clock));
      }
    }
  }

  /** Retoma uma tarefa bloqueada sem duplicar geração que já esteja solicitada ou em execução. */
  private boolean shouldRequestCreative(AgentTask task, Experiment experiment) {
    if ("PENDING".equals(task.getStatus())) {
      return true;
    }
    CreativeGenerationStatus status = experiment.getCreativeGenerationStatus();
    int pendingQuantity =
        experiment.getCreativesToGenerate() == null ? 0 : experiment.getCreativesToGenerate();
    return "IN_PROGRESS".equals(task.getStatus())
        && pendingQuantity <= 0
        && status != CreativeGenerationStatus.REQUESTED
        && status != CreativeGenerationStatus.PROCESSING;
  }

  /** Encerra a tarefa da Têmis somente após o worker materializar o anúncio solicitado. */
  @Transactional
  public void completeForExperiment(Long experimentId) {
    updateTask(experimentId, "COMPLETED");
  }

  /** Bloqueia a tarefa quando a materialização falha, preservando a causa no experimento. */
  @Transactional
  public void blockForExperiment(Long experimentId) {
    updateTask(experimentId, "BLOCKED");
  }

  /** Solicita exatamente uma alternativa sem substituir criativos ou histórico existentes. */
  private void requestOneCreative(Experiment experiment) {
    experiment.setCreativesToGenerate(1);
    experiment.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
    experiment.setCreativeGenerationStatus(CreativeGenerationStatus.REQUESTED);
    experiment.setCreativeGenerationRequestedAt(Instant.now(clock));
    experiment.setCreativeGenerationStartedAt(null);
    experiment.setCreativeGenerationFinishedAt(null);
    experiment.setCreativeGenerationError(null);
  }

  /** Atualiza a tarefa ativa vinculada ao experimento após callback do executor. */
  private void updateTask(Long experimentId, String status) {
    String reference = "experiment:" + experimentId;
    taskRepository
        .findTopBySourceReferenceOrderByUpdatedAtDescIdDesc(reference)
        .ifPresent(
            task -> {
              if (TEMIS_AGENT_KEY.equals(task.getAssignedAgent().getAgentKey())
                  && "IN_PROGRESS".equals(task.getStatus())) {
                task.setStatus(status);
                task.setUpdatedAt(Instant.now(clock));
              }
            });
  }

  /** Bloqueia tarefa cuja referência aponta para experimento inexistente. */
  private void block(AgentTask task) {
    task.setStatus("BLOCKED");
    task.setUpdatedAt(Instant.now(clock));
  }

  /** Extrai o identificador somente da referência canônica, sem inferir pelo texto livre. */
  private Long experimentId(String sourceReference) {
    if (sourceReference == null) {
      return null;
    }
    Matcher matcher = EXPERIMENT_REFERENCE.matcher(sourceReference.trim());
    return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
  }
}

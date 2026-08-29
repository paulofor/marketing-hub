package com.marketinghub.experiment.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskBlockerGuidanceRequest;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskHelpLinkRequest;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.agenttask.FailAgentTaskRequest;
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

/** Responsabilidade: ligar tarefas criativas de Dédalo à fila canônica de materialização. */
@Service
public class DedaloCreativeTaskOrchestrationService {
  private static final String DEDALO_AGENT_KEY = "landing-generator";
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:(\\d+)$");
  private final AgentTaskRepository taskRepository;
  private final ExperimentRepository experimentRepository;
  private final AgentTaskService agentTaskService;
  private final Clock clock;

  /** Configura a orquestração com as fontes de verdade de tarefas e experimentos. */
  @Autowired
  public DedaloCreativeTaskOrchestrationService(
      AgentTaskRepository taskRepository,
      ExperimentRepository experimentRepository,
      AgentTaskService agentTaskService) {
    this(taskRepository, experimentRepository, agentTaskService, Clock.systemUTC());
  }

  /** Permite validar transições temporais de forma determinística. */
  DedaloCreativeTaskOrchestrationService(
      AgentTaskRepository taskRepository,
      ExperimentRepository experimentRepository,
      AgentTaskService agentTaskService,
      Clock clock) {
    this.taskRepository = taskRepository;
    this.experimentRepository = experimentRepository;
    this.agentTaskService = agentTaskService;
    this.clock = clock;
  }

  /** Converte tarefas pendentes de Dédalo em solicitações idempotentes na fila do AI Worker. */
  @Transactional
  public void reconcilePendingTasks() {
    List<AgentTask> tasks =
        taskRepository.findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
            DEDALO_AGENT_KEY, "WORK", List.of("PENDING", "IN_PROGRESS"));
    for (AgentTask task : tasks) {
      Long experimentId = experimentId(task.getSourceReference());
      if (experimentId == null) {
        continue;
      }
      Experiment experiment = experimentRepository.findById(experimentId).orElse(null);
      if (experiment == null) {
        blockMissingExperiment(task, experimentId);
        continue;
      }
      if (shouldRequestCreative(task, experiment)) {
        requestOneCreative(experiment);
        task.setStatus("IN_PROGRESS");
        Instant now = Instant.now(clock);
        if (task.getReceivedAt() == null) task.setReceivedAt(now);
        task.setUpdatedAt(now);
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

  /** Conclui a tarefa de Dédalo somente após o callback de materialização do executor. */
  @Transactional
  public void completeForExperiment(
      Long experimentId, AgentTaskExecutionAuditRequest executionAudit) {
    activeTask(experimentId)
        .ifPresent(
            task ->
                agentTaskService.completeClaimedProcessTask(
                    DEDALO_AGENT_KEY,
                    task.getId(),
                    new CompleteAgentTaskRequest(
                        "{\"decision\":\"CREATIVES_MATERIALIZED\",\"experimentId\":"
                            + experimentId
                            + "}",
                        "{\"creativeGenerationStatus\":\"COMPLETED\",\"experimentId\":"
                            + experimentId
                            + "}",
                        null,
                        executionAudit)));
  }

  /** Bloqueia a tarefa quando a materialização falha, preservando a causa no experimento. */
  @Transactional
  public void blockForExperiment(
      Long experimentId, String error, AgentTaskExecutionAuditRequest executionAudit) {
    String normalizedError =
        error == null || error.isBlank()
            ? "O executor não informou a causa da falha na geração de criativos."
            : error.trim();
    activeTask(experimentId)
        .ifPresent(
            task ->
                agentTaskService.failClaimedProcessTask(
                    DEDALO_AGENT_KEY,
                    task.getId(),
                    new FailAgentTaskRequest(
                        normalizedError,
                        null,
                        "{\"creativeGenerationStatus\":\"FAILED\",\"experimentId\":"
                            + experimentId
                            + "}",
                        null,
                        executionAudit,
                        blockerGuidance(experimentId, normalizedError))));
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

  /** Localiza somente a tarefa ativa de Dédalo vinculada ao experimento do callback. */
  private java.util.Optional<AgentTask> activeTask(Long experimentId) {
    String reference = "experiment:" + experimentId;
    return taskRepository
        .findTopBySourceReferenceOrderByUpdatedAtDescIdDesc(reference)
        .filter(task -> DEDALO_AGENT_KEY.equals(task.getAssignedAgent().getAgentKey()))
        .filter(task -> "IN_PROGRESS".equals(task.getStatus()));
  }

  /** Bloqueia tarefa cuja referência aponta para experimento inexistente. */
  private void blockMissingExperiment(AgentTask task, Long experimentId) {
    Instant now = Instant.now(clock);
    task.setStatus("IN_PROGRESS");
    if (task.getReceivedAt() == null) task.setReceivedAt(now);
    task.setUpdatedAt(now);
    taskRepository.save(task);
    String error = "Experimento " + experimentId + " não foi encontrado.";
    agentTaskService.failClaimedProcessTask(
        DEDALO_AGENT_KEY,
        task.getId(),
        new FailAgentTaskRequest(
            error,
            null,
            "{\"experimentId\":" + experimentId + "}",
            null,
            null,
            blockerGuidance(experimentId, error)));
  }

  /** Monta uma correção acionável para o histórico e para a retomada da tarefa. */
  private AgentTaskBlockerGuidanceRequest blockerGuidance(Long experimentId, String error) {
    return new AgentTaskBlockerGuidanceRequest(
        "TECHNICAL_FAILURE",
        "Corrija a geração de criativos e reinicie a tarefa de Dédalo: " + error,
        List.of(
            new AgentTaskHelpLinkRequest("Abrir experimento", "/experiments/" + experimentId),
            new AgentTaskHelpLinkRequest("Abrir tarefas dos agentes", "/agent-tasks")));
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

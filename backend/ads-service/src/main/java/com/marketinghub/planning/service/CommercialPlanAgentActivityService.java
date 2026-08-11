package com.marketinghub.planning.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto.Entry;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar a prestação de contas dos agentes por plano comercial. */
@Service
public class CommercialPlanAgentActivityService {
  private final AgentTaskRepository taskRepository;
  private final VideoProductionCycleRepository videoCycleRepository;
  private final GeraLandingStageExecutionRepository landingRepository;
  private final CommercialPlanVersionService versionService;

  /** Configura as fontes persistidas da visão unificada do plano. */
  public CommercialPlanAgentActivityService(
      AgentTaskRepository taskRepository,
      VideoProductionCycleRepository videoCycleRepository,
      GeraLandingStageExecutionRepository landingRepository,
      CommercialPlanVersionService versionService) {
    this.taskRepository = taskRepository;
    this.videoCycleRepository = videoCycleRepository;
    this.landingRepository = landingRepository;
    this.versionService = versionService;
  }

  /** Consolida tarefas, gates, landing e ciclos de vídeo sem misturar produtos. */
  @Transactional(readOnly = true)
  public CommercialPlanAgentActivityDto activity(CommercialPlan plan) {
    List<AgentTask> tasks =
        taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:" + plan.getId() + "@v");
    List<VideoProductionCycle> cycles =
        videoCycleRepository.findByCommercialPlanIdOrderByUpdatedAtDesc(plan.getId());
    List<Entry> entries = new ArrayList<>();
    tasks.forEach(task -> entries.add(taskEntry(task)));
    cycles.forEach(cycle -> entries.add(videoEntry(cycle, "financial-agent", "Plutus")));
    cycles.forEach(cycle -> entries.add(videoEntry(cycle, "videomaker", "Apolo")));
    if (plan.getExperiment() != null) {
      landingRepository
          .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
              plan.getExperiment().getId(), "landing-generation-agent-v1")
          .ifPresent(execution -> entries.add(landingEntry(execution)));
    }
    entries.sort(
        Comparator.comparing(Entry::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())));
    BigDecimal videoBudget =
        cycles.stream()
            .map(VideoProductionCycle::getBudgetLimitUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal videoCost =
        cycles.stream()
            .map(VideoProductionCycle::getKnownCostUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long openTasks =
        tasks.stream()
            .filter(task -> List.of("PENDING", "IN_PROGRESS", "BLOCKED").contains(task.getStatus()))
            .count();
    long decisions =
        tasks.stream()
            .filter(
                task ->
                    "GATE_DECISION".equals(task.getTaskKind())
                        && "PENDING".equals(task.getGateStatus()))
            .count();
    return new CommercialPlanAgentActivityDto(
        plan.getId(),
        versionService.current(plan.getId()).versionNumber(),
        plan.getMaxBudget(),
        plan.getActualCampaignCost(),
        plan.getActualAiCost(),
        plan.getActualTotalCost(),
        plan.getActualRevenue(),
        videoBudget,
        videoCost,
        openTasks,
        decisions,
        List.copyOf(entries));
  }

  /** Converte uma tarefa ou gate da mesa em registro do plano. */
  private Entry taskEntry(AgentTask task) {
    boolean decision =
        "GATE_DECISION".equals(task.getTaskKind()) && "PENDING".equals(task.getGateStatus());
    return new Entry(
        "GATE_DECISION".equals(task.getTaskKind()) ? "GATE" : "TASK",
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        task.getTitle(),
        task.getStatus(),
        task.getDescription(),
        "BLOCKED".equals(task.getStatus()) ? "Tarefa bloqueada na mesa do agente." : null,
        decision,
        decision ? "Decisão pendente no gate " + task.getGateCode() + "." : null,
        task.getSourceReference(),
        null,
        null,
        task.getGateStatus(),
        task.getUpdatedAt());
  }

  /** Converte o ciclo audiovisual na visão financeira ou criativa correspondente. */
  private Entry videoEntry(VideoProductionCycle cycle, String agentKey, String nickname) {
    boolean plutus = "financial-agent".equals(agentKey);
    boolean pending = "PENDING_FINANCIAL_REVIEW".equals(cycle.getStatus());
    return new Entry(
        plutus ? "FINANCIAL_GATE" : "VIDEO_PRODUCTION",
        agentKey,
        nickname,
        plutus
            ? "Controle financeiro do ciclo de vídeo #" + cycle.getId()
            : "Produção audiovisual do ciclo #" + cycle.getId(),
        pending && !plutus ? "WAITING" : cycle.getStatus(),
        cycle.getFinancialReason(),
        "FINANCIAL_BLOCKED".equals(cycle.getStatus()) ? cycle.getFinancialReason() : null,
        pending && plutus,
        pending && plutus ? "Plutus precisa decidir o orçamento antes da geração." : null,
        "video-production-cycle:" + cycle.getId(),
        cycle.getBudgetLimitUsd(),
        cycle.getKnownCostUsd(),
        cycle.getFinancialDecision(),
        cycle.getUpdatedAt());
  }

  /** Converte a execução mais recente de Dédalo vinculada ao experimento do plano. */
  private Entry landingEntry(GeraLandingStageExecution execution) {
    return new Entry(
        "LANDING",
        "landing-generator",
        "Dédalo",
        "Landing do experimento #" + execution.getExperimentId(),
        execution.getStatus(),
        "Etapa " + execution.getStageCode(),
        execution.getErrorMessage(),
        false,
        null,
        "geralanding-experiment:" + execution.getExperimentId(),
        null,
        execution.getCostUsd(),
        null,
        latestActivity(execution));
  }

  /** Seleciona o instante mais recente persistido na execução de landing. */
  private Instant latestActivity(GeraLandingStageExecution execution) {
    if (execution.getCompletedAt() != null) return execution.getCompletedAt();
    if (execution.getProcessingStartedAt() != null) return execution.getProcessingStartedAt();
    return execution.getExecutionRequestedAt();
  }
}

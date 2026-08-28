package com.marketinghub.planning.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto.Entry;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar a prestação de contas dos agentes por plano comercial. */
@Service
public class CommercialPlanAgentActivityService {
  private static final Set<String> IRIS_JOURNEY_ACTIVITY_IDS =
      Set.of("select", "strategy", "compose", "html", "customer", "commercial");
  private static final Set<String> LEGACY_JOURNEY_ACTIVITY_IDS =
      Set.of("html", "customer", "commercial");
  private final AgentTaskRepository taskRepository;
  private final VideoProductionCycleRepository videoCycleRepository;
  private final GeraLandingStageExecutionRepository landingRepository;
  private final ExperimentStrategistExecutionRepository strategistRepository;
  private final FinancialAgentExecutionRepository financialRepository;
  private final GrowthOperatorExecutionRepository growthOperatorRepository;
  private final CommercialPlanVersionService versionService;

  /** Configura as fontes persistidas da visão unificada do plano. */
  public CommercialPlanAgentActivityService(
      AgentTaskRepository taskRepository,
      VideoProductionCycleRepository videoCycleRepository,
      GeraLandingStageExecutionRepository landingRepository,
      ExperimentStrategistExecutionRepository strategistRepository,
      FinancialAgentExecutionRepository financialRepository,
      GrowthOperatorExecutionRepository growthOperatorRepository,
      CommercialPlanVersionService versionService) {
    this.taskRepository = taskRepository;
    this.videoCycleRepository = videoCycleRepository;
    this.landingRepository = landingRepository;
    this.strategistRepository = strategistRepository;
    this.financialRepository = financialRepository;
    this.growthOperatorRepository = growthOperatorRepository;
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
    journeyHomologationEntry(tasks).ifPresent(entries::add);
    cycles.forEach(cycle -> entries.add(videoEntry(cycle, "financial-agent", "Plutus")));
    cycles.forEach(cycle -> entries.add(videoEntry(cycle, "videomaker", "Apolo")));
    strategistRepository
        .findByCommercialPlanIdOrderByCreatedAtDesc(plan.getId())
        .forEach(execution -> entries.add(strategistEntry(execution)));
    financialRepository
        .findByCommercialPlanIdOrderByCreatedAtDesc(plan.getId())
        .forEach(execution -> entries.add(financialEntry(execution)));
    growthOperatorRepository
        .findByCommercialPlanIdOrderByCreatedAtDesc(plan.getId())
        .forEach(execution -> entries.add(growthOperatorEntry(execution)));
    var planExperiments = new LinkedHashSet<>(plan.getExperiments());
    if (plan.getExperiment() != null) planExperiments.add(plan.getExperiment());
    for (var experiment : planExperiments) {
      List<GeraLandingStageExecution> landingExecutions =
          landingRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
              experiment.getId(), "landing-generation-agent-v1");
      landingExecutions.stream()
          .findFirst()
          .ifPresent(execution -> entries.add(landingEntry(execution)));
      landingExecutions.stream()
          .filter(this::isJourneyHomologation)
          .findFirst()
          .filter(
              execution ->
                  entries.stream()
                      .noneMatch(
                          entry ->
                              entry.sourceReference().endsWith(execution.getAutonomousCycleId())))
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
        null,
        "BLOCKED".equals(task.getStatus()) ? "Tarefa bloqueada na mesa do agente." : null,
        decision,
        decision ? "Decisão pendente no gate " + task.getGateCode() + "." : null,
        task.getSourceReference(),
        null,
        null,
        task.getGateStatus(),
        task.getUpdatedAt());
  }

  /** Consolida a cadeia oficial da landing sem confundir conclusão técnica isolada. */
  private Optional<Entry> journeyHomologationEntry(List<AgentTask> tasks) {
    Map<String, List<AgentTask>> byExecution = new LinkedHashMap<>();
    tasks.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task -> "landing-page-generation".equals(task.getProcessDefinition().getProcessCode()))
        .filter(task -> IRIS_JOURNEY_ACTIVITY_IDS.contains(task.getProcessActivityId()))
        .filter(task -> task.getSourceReference() != null)
        .forEach(
            task ->
                byExecution
                    .computeIfAbsent(task.getSourceReference(), ignored -> new ArrayList<>())
                    .add(task));
    return byExecution.entrySet().stream()
        .max(Comparator.comparing(entry -> latestActivity(entry.getValue())))
        .map(entry -> aggregateJourneyEntry(entry.getKey(), entry.getValue()));
  }

  /** Produz um único estado funcional a partir da tentativa atual de cada gate obrigatório. */
  private Entry aggregateJourneyEntry(String sourceReference, List<AgentTask> tasks) {
    Map<String, AgentTask> latestByActivity = new LinkedHashMap<>();
    tasks.stream()
        .sorted(
            Comparator.comparing(
                    AgentTask::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AgentTask::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
        .forEach(task -> latestByActivity.put(task.getProcessActivityId(), task));
    boolean irisExecution =
        tasks.stream()
            .filter(task -> task.getAssignedAgent() != null)
            .anyMatch(
                task -> "communication-director".equals(task.getAssignedAgent().getAgentKey()));
    Set<String> requiredActivities =
        irisExecution ? IRIS_JOURNEY_ACTIVITY_IDS : LEGACY_JOURNEY_ACTIVITY_IDS;
    boolean complete =
        requiredActivities.stream()
            .allMatch(
                activityId ->
                    latestByActivity.containsKey(activityId)
                        && "COMPLETED".equals(latestByActivity.get(activityId).getStatus()));
    boolean blocked =
        latestByActivity.values().stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()));
    boolean processing =
        latestByActivity.values().stream().anyMatch(task -> "IN_PROGRESS".equals(task.getStatus()));
    String status =
        complete ? "COMPLETED" : blocked ? "BLOCKED" : processing ? "IN_PROGRESS" : "PENDING";
    BigDecimal cost =
        latestByActivity.values().stream()
            .map(AgentTask::getEstimatedCostUsd)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    String detail =
        requiredActivities.stream()
            .sorted()
            .map(
                activityId ->
                    activityId
                        + "="
                        + Optional.ofNullable(latestByActivity.get(activityId))
                            .map(AgentTask::getStatus)
                            .orElse("AUSENTE"))
            .collect(java.util.stream.Collectors.joining(", "));
    String difficulty =
        latestByActivity.values().stream()
            .filter(task -> "BLOCKED".equals(task.getStatus()))
            .map(AgentTask::getExecutionError)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    return new Entry(
        "JOURNEY_HOMOLOGATION",
        irisExecution ? "communication-director" : "landing-generator",
        irisExecution ? "Íris" : "Dédalo",
        "Homologação oficial da landing",
        status,
        detail,
        null,
        difficulty,
        false,
        null,
        "commercial-plan-journey-homologation:" + sourceReference,
        null,
        cost.signum() == 0 ? null : cost,
        null,
        latestActivity(latestByActivity.values().stream().toList()));
  }

  /** Seleciona o último instante auditável de uma coleção de tarefas. */
  private Instant latestActivity(List<AgentTask> tasks) {
    return tasks.stream()
        .map(AgentTask::getUpdatedAt)
        .filter(java.util.Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(Instant.EPOCH);
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
        plutus ? cycle.getFinancialReason() : null,
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
    boolean journeyHomologation = isJourneyHomologation(execution);
    return new Entry(
        journeyHomologation ? "LANDING_TECHNICAL_HOMOLOGATION" : "LANDING",
        "landing-generator",
        "Dédalo",
        journeyHomologation
            ? "Homologação da jornada do experimento #" + execution.getExperimentId()
            : "Landing do experimento #" + execution.getExperimentId(),
        execution.getStatus(),
        "Etapa " + execution.getStageCode(),
        null,
        execution.getErrorMessage(),
        false,
        null,
        journeyHomologation
            ? "commercial-plan-journey-homologation:" + execution.getAutonomousCycleId()
            : "geralanding-experiment:" + execution.getExperimentId(),
        null,
        execution.getCostUsd(),
        null,
        latestActivity(execution));
  }

  /** Identifica exclusivamente ciclos criados pelo contrato de homologação comercial. */
  private boolean isJourneyHomologation(GeraLandingStageExecution execution) {
    return execution.getAutonomousCycleId() != null
        && execution.getAutonomousCycleId().startsWith("cph-");
  }

  /** Converte a pesquisa estratégica de Atena e preserva seu parecer final estruturado. */
  private Entry strategistEntry(ExperimentStrategistExecution execution) {
    return new Entry(
        "STRATEGIC_OPINION",
        "experiment-strategist",
        "Atena",
        "Parecer estratégico #" + execution.getId(),
        execution.getStatus().name(),
        execution.getResearchQuestion(),
        execution.getRecommendationJson(),
        execution.getErrorMessage(),
        false,
        null,
        "experiment-strategist-execution:" + execution.getId(),
        null,
        execution.getEstimatedCost(),
        null,
        execution.getFinishedAt() != null ? execution.getFinishedAt() : execution.getUpdatedAt());
  }

  /** Converte a análise de Plutus e preserva o relatório financeiro final. */
  private Entry financialEntry(FinancialAgentExecution execution) {
    return new Entry(
        "FINANCIAL_OPINION",
        "financial-agent",
        "Plutus",
        "Parecer financeiro #" + execution.getId(),
        execution.getStatus().name(),
        execution.getProjectionRequest(),
        execution.getDailyReport() != null
            ? execution.getDailyReport()
            : execution.getReconciliationJson(),
        execution.getErrorMessage(),
        false,
        null,
        "financial-agent-execution:" + execution.getId(),
        null,
        execution.getEstimatedCost(),
        null,
        execution.getFinishedAt() != null ? execution.getFinishedAt() : execution.getUpdatedAt());
  }

  /** Converte a coordenação de Hermes e preserva diagnóstico e recomendação finais. */
  private Entry growthOperatorEntry(GrowthOperatorExecution execution) {
    String opinion =
        execution.getDailyReport() != null
            ? execution.getDailyReport()
            : execution.getRecommendedAction();
    return new Entry(
        "GROWTH_OPINION",
        "growth-operator",
        "Hermes",
        "Parecer de crescimento #" + execution.getId(),
        execution.getStatus().name(),
        execution.getObjective(),
        opinion,
        execution.getErrorMessage() != null ? execution.getErrorMessage() : execution.getBlocker(),
        false,
        null,
        "growth-operator-execution:" + execution.getId(),
        null,
        execution.getEstimatedCost(),
        execution.getRecommendedDecision() != null
            ? execution.getRecommendedDecision().name()
            : null,
        execution.getFinishedAt() != null ? execution.getFinishedAt() : execution.getUpdatedAt());
  }

  /** Seleciona o instante mais recente persistido na execução de landing. */
  private Instant latestActivity(GeraLandingStageExecution execution) {
    if (execution.getCompletedAt() != null) return execution.getCompletedAt();
    if (execution.getProcessingStartedAt() != null) return execution.getProcessingStartedAt();
    return execution.getExecutionRequestedAt();
  }
}

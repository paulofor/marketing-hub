package com.marketinghub.experimentstrategist.service;

import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService;
import com.marketinghub.growthoperator.service.GrowthOperatorService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: consolidar o contexto interno somente leitura do Estrategista de Experimentos.
 */
@Service
public class ExperimentStrategistContextService {
  private final CommercialPlanService commercialPlanService;
  private final GrowthOperatorService growthOperatorService;
  private final JdbcTemplate jdbc;
  private final ExperimentStrategistMemoryService memoryService;

  /** Configura as fontes canônicas usadas na pesquisa estratégica. */
  public ExperimentStrategistContextService(
      CommercialPlanService commercialPlanService,
      GrowthOperatorService growthOperatorService,
      JdbcTemplate jdbc,
      ExperimentStrategistMemoryService memoryService) {
    this.commercialPlanService = commercialPlanService;
    this.growthOperatorService = growthOperatorService;
    this.jdbc = jdbc;
    this.memoryService = memoryService;
  }

  /** Entrega planejamento, sessões, funil, vídeos e aprendizados sem permitir mutações. */
  @Transactional(readOnly = true)
  public Map<String, Object> researchContext(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    LinkedHashMap<String, Object> context = new LinkedHashMap<>();
    context.put("authorityMode", "READ_ONLY_RESEARCH");
    context.put("commercialPlan", planContext(plan));
    context.put("sessionsAndFunnel", growthOperatorService.sessionIntelligence(planId, 2000));
    context.put("videoStrategy", growthOperatorService.videoStrategyIntelligence(planId));
    context.put("learnings", learnings(plan));
    context.put("behavioralMemory", memoryService.activeForPlan(planId));
    context.put("behavioralScienceLibrary", "classpath:behavioral-science/v1/library.md");
    context.put(
        "publicResearchContract",
        Map.of(
            "required",
            true,
            "minimumSources",
            2,
            "requiredFields",
            List.of("url", "title", "accessedAt", "learning")));
    context.put(
        "prohibitedActions",
        List.of("PRICE", "CAMPAIGN", "BUDGET", "PUBLICATION", "MASS_COMMUNICATION"));
    return context;
  }

  /** Resume apenas os dados comerciais necessários para desenhar um experimento. */
  private Map<String, Object> planContext(CommercialPlan plan) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("id", plan.getId());
    result.put("name", plan.getName());
    result.put("objective", plan.getCommercialObjective());
    result.put("currentBlocker", plan.getCurrentBlocker());
    result.put("mainMetric", plan.getMainMetric());
    result.put("successCriteria", plan.getSuccessCriteria());
    result.put("stopCriteria", plan.getStopCriteria());
    result.put("maxBudget", plan.getMaxBudget());
    result.put("actualTotalCost", plan.getActualTotalCost());
    result.put("actualRevenue", plan.getActualRevenue());
    result.put("experimentId", plan.getExperiment() == null ? null : plan.getExperiment().getId());
    return result;
  }

  /** Busca aprendizados fechados do experimento sem recomputá-los no agente. */
  private List<Map<String, Object>> learnings(CommercialPlan plan) {
    if (plan.getExperiment() == null) {
      return List.of();
    }
    return jdbc.queryForList(
        "SELECT id, stage, primary_metric, metric_signal, summary, what_worked, what_blocked, next_test, completed_at "
            + "FROM experiment_learning WHERE experiment_id = ? ORDER BY completed_at DESC, id DESC LIMIT 30",
        plan.getExperiment().getId());
  }
}

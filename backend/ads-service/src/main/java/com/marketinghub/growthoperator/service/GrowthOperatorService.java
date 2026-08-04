package com.marketinghub.growthoperator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.video.service.ExperimentVideoPerformanceDashboardService;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import com.marketinghub.growthoperator.service.result.CompleteGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.result.FailGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.start.StartGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.view.GrowthOperatorExecutionResponse;
import com.marketinghub.growthoperator.service.view.GrowthOperatorMcpToolResponse;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: orquestrar diagnosticos de crescimento somente leitura e sua auditoria. */
@Service
public class GrowthOperatorService {
  private static final Logger log = LoggerFactory.getLogger(GrowthOperatorService.class);
  private static final String READ_ONLY = "READ_ONLY_DIAGNOSIS";
  private static final int SESSION_EVENT_LIMIT = 2000;
  private static final int MEMORY_TIMELINE_LIMIT = 30;
  private static final List<GrowthOperatorMcpToolResponse> MCP_TOOLS =
      List.of(
          new GrowthOperatorMcpToolResponse(
              "consultar_planejamento",
              "Consulta o planejamento comercial e suas metas atuais.",
              "SOMENTE_LEITURA",
              "Planejamento Comercial",
              Map.of()),
          new GrowthOperatorMcpToolResponse(
              "consultar_funil",
              "Consulta o funil consolidado do experimento vinculado ao planejamento.",
              "SOMENTE_LEITURA",
              "Experimentos",
              Map.of()),
          new GrowthOperatorMcpToolResponse(
              "consultar_sessoes",
              "Consulta jornadas e eventos anonimizados do planejamento.",
              "SOMENTE_LEITURA",
              "Inteligencia de sessoes",
              Map.of("eventLimit", "Inteiro entre 1 e 2000; padrao 2000")),
          new GrowthOperatorMcpToolResponse(
              "consultar_campanhas",
              "Consulta as campanhas Meta do experimento vinculado ao planejamento.",
              "SOMENTE_LEITURA",
              "Campanhas Meta",
              Map.of()),
          new GrowthOperatorMcpToolResponse(
              "consultar_memoria",
              "Consulta o historico auditavel dos ciclos do Operador.",
              "SOMENTE_LEITURA",
              "Memoria do Operador",
              Map.of()),
          new GrowthOperatorMcpToolResponse(
              "consultar_estrategia_videos",
              "Consulta estrategia, custo, progressao e aprendizados dos videos do experimento.",
              "SOMENTE_LEITURA",
              "Estrategia e aprendizado de videos",
              Map.of()));
  private final GrowthOperatorExecutionRepository repository;
  private final CommercialPlanService commercialPlanService;
  private final ExperimentFunnelService experimentFunnelService;
  private final VideoProjectRepository videoProjectRepository;
  private final ExperimentVideoPerformanceDashboardService videoPerformanceService;
  private final ObjectMapper objectMapper;

  public GrowthOperatorService(
      GrowthOperatorExecutionRepository repository,
      CommercialPlanService commercialPlanService,
      ExperimentFunnelService experimentFunnelService,
      VideoProjectRepository videoProjectRepository,
      ExperimentVideoPerformanceDashboardService videoPerformanceService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.commercialPlanService = commercialPlanService;
    this.experimentFunnelService = experimentFunnelService;
    this.videoProjectRepository = videoProjectRepository;
    this.videoPerformanceService = videoPerformanceService;
    this.objectMapper = objectMapper;
  }

  /** Cria uma pendencia imutavelmente limitada a leitura e diagnostico. */
  @Transactional
  public GrowthOperatorExecutionResponse start(Long planId, StartGrowthOperatorRequest request) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    GrowthOperatorExecution execution = new GrowthOperatorExecution();
    execution.setCommercialPlan(plan);
    execution.setWeekNumber(request.weekNumber() == null ? 1 : request.weekNumber());
    execution.setStatus(GrowthOperatorExecutionStatus.PENDING);
    execution.setAuthorityMode(READ_ONLY);
    execution.setObjective(
        hasText(request.objective()) ? request.objective() : defaultObjective(plan));
    execution.setBlocker(plan.getCurrentBlocker());
    execution.setEvidenceSnapshot(
        buildEvidenceSnapshot(plan, repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId)));
    execution.setCycleNumber(nextCycleNumber(planId));
    execution.setAutomaticCycle(false);
    return toResponse(repository.save(execution));
  }

  /** Lista o historico de diagnosticos de um planejamento. */
  @Transactional(readOnly = true)
  public List<GrowthOperatorExecutionResponse> list(Long planId) {
    commercialPlanService.getPlan(planId);
    return repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Lista o catalogo MCP efetivamente autorizado para diagnosticos do Operador. */
  public List<GrowthOperatorMcpToolResponse> listMcpTools() {
    return MCP_TOOLS;
  }

  /** Consulta as sessoes atuais por API sem expor banco ou dados pessoais ao worker. */
  @Transactional(readOnly = true)
  public Map<String, Object> sessionIntelligence(Long planId, int requestedEventLimit) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    if (plan.getExperiment() == null) {
      return Map.of("available", false, "reason", "PLAN_WITHOUT_EXPERIMENT", "planId", planId);
    }
    int eventLimit = Math.max(1, Math.min(requestedEventLimit, SESSION_EVENT_LIMIT));
    Long experimentId = plan.getExperiment().getId();
    LinkedHashMap<String, Object> intelligence = new LinkedHashMap<>();
    intelligence.put("available", true);
    intelligence.put("planId", planId);
    intelligence.put("experimentId", experimentId);
    intelligence.put("requestedEventLimit", requestedEventLimit);
    intelligence.put("appliedEventLimit", eventLimit);
    intelligence.put(
        "landingAnalytics",
        experimentFunnelService.buildDetailedAnalyticsEvidence(experimentId, eventLimit));
    intelligence.put(
        "pdeAnalytics", experimentFunnelService.buildDetailedPdeAnalyticsEvidence(experimentId));
    return intelligence;
  }

  /** Consolida estrategia, custos de campanha, progressao e aprendizado dos videos do plano. */
  @Transactional(readOnly = true)
  public Map<String, Object> videoStrategyIntelligence(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    if (plan.getExperiment() == null) {
      return Map.of("available", false, "reason", "PLAN_WITHOUT_EXPERIMENT", "planId", planId);
    }
    Long experimentId = plan.getExperiment().getId();
    List<Map<String, Object>> strategies =
        videoProjectRepository.findByExperimentIdOrderByUpdatedAtDesc(experimentId).stream()
            .map(this::toVideoStrategyEvidence)
            .toList();
    LinkedHashMap<String, Object> intelligence = new LinkedHashMap<>();
    intelligence.put("available", true);
    intelligence.put("planId", planId);
    intelligence.put("experimentId", experimentId);
    intelligence.put("strategyCount", strategies.size());
    intelligence.put("strategies", strategies);
    intelligence.put("performance", videoPerformanceService.summarize(experimentId));
    intelligence.put("commercialCost", plan.getActualCampaignCost());
    intelligence.put("totalPlanCost", plan.getActualTotalCost());
    intelligence.put("revenue", plan.getActualRevenue());
    return intelligence;
  }

  /** Converte um projeto em evidencia comercial sem expor campos internos desnecessarios. */
  private Map<String, Object> toVideoStrategyEvidence(VideoProject project) {
    LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("projectId", project.getId());
    evidence.put("title", project.getTitle());
    evidence.put("strategyGroupKey", project.getStrategyGroupKey());
    evidence.put("strategyRole", project.getStrategyRole());
    evidence.put("funnelStage", project.getFunnelStage());
    evidence.put("primaryMetric", project.getPrimaryMetric());
    evidence.put("commercialHypothesis", project.getCommercialHypothesis());
    evidence.put("persuasionFramework", project.getPersuasionFramework());
    evidence.put("scientificBasis", project.getScientificBasis());
    evidence.put("measurementPlan", project.getMeasurementPlan());
    evidence.put("resultsSnapshot", project.getResultsSnapshot());
    evidence.put("learningDecision", project.getLearningDecision());
    evidence.put("confirmedLearning", project.getConfirmedLearning());
    evidence.put("nextVersionRecommendation", project.getNextVersionRecommendation());
    evidence.put("status", project.getStatus());
    evidence.put("updatedAt", project.getUpdatedAt());
    return evidence;
  }

  /** Reserva a pendencia mais antiga para um unico worker. */
  @Transactional
  public GrowthOperatorExecutionResponse claimPending() {
    GrowthOperatorExecution execution =
        repository
            .findByStatusOrderByCreatedAtAsc(
                GrowthOperatorExecutionStatus.PENDING, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nenhum diagnostico pendente."));
    execution.setStatus(GrowthOperatorExecutionStatus.RUNNING);
    execution.setStartedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Cria o proximo ciclo automatico somente quando o backend considera a cadencia vencida. */
  @Transactional
  public GrowthOperatorExecutionResponse ensureAutomaticCycle(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    var latest = repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(planId);
    Instant cutoff = Instant.now().minusSeconds(30 * 60L);
    if (latest.isPresent()
        && (latest.get().getStatus() == GrowthOperatorExecutionStatus.PENDING
            || latest.get().getStatus() == GrowthOperatorExecutionStatus.RUNNING
            || latest.get().getCreatedAt().isAfter(cutoff))) {
      return toResponse(latest.get());
    }
    GrowthOperatorExecution execution = new GrowthOperatorExecution();
    execution.setCommercialPlan(plan);
    execution.setWeekNumber(1);
    execution.setStatus(GrowthOperatorExecutionStatus.PENDING);
    execution.setAuthorityMode(READ_ONLY);
    execution.setObjective(defaultObjective(plan));
    execution.setBlocker(plan.getCurrentBlocker());
    execution.setEvidenceSnapshot(
        buildEvidenceSnapshot(plan, repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId)));
    execution.setCycleNumber(nextCycleNumber(planId));
    execution.setAutomaticCycle(true);
    return toResponse(repository.save(execution));
  }

  /** Registra somente o diagnostico, sem executar nem persistir a acao recomendada no plano. */
  @Transactional
  public GrowthOperatorExecutionResponse complete(Long id, CompleteGrowthOperatorRequest request) {
    GrowthOperatorExecution execution = get(id);
    if (!READ_ONLY.equals(execution.getAuthorityMode())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Autoridade de execucao invalida.");
    }
    execution.setAlternativesJson(request.alternativesJson());
    execution.setDiagnosisJson(request.diagnosisJson());
    execution.setRawModelResponse(request.rawModelResponse());
    execution.setRecommendedDecision(request.recommendedDecision());
    execution.setRecommendedAction(request.recommendedAction());
    execution.setDailyReport(request.dailyReport());
    execution.setModel(request.model());
    execution.setInputTokens(request.inputTokens());
    execution.setOutputTokens(request.outputTokens());
    execution.setEstimatedCost(request.estimatedCost());
    execution.setStatus(GrowthOperatorExecutionStatus.COMPLETED);
    execution.setFinishedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Registra uma falha tecnica com contexto para nova investigacao. */
  @Transactional
  public GrowthOperatorExecutionResponse fail(Long id, FailGrowthOperatorRequest request) {
    GrowthOperatorExecution execution = get(id);
    execution.setErrorMessage(request.errorMessage());
    execution.setStatus(GrowthOperatorExecutionStatus.FAILED);
    execution.setFinishedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Busca uma execucao existente. */
  private GrowthOperatorExecution get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Execucao do operador nao encontrada: " + id));
  }

  /** Monta o objetivo padrao a partir da meta e do gargalo persistidos. */
  private String defaultObjective(CommercialPlan plan) {
    return "Diagnosticar a primeira quebra que impede a meta: "
        + text(plan.getCommercialObjective())
        + ". Gargalo atual: "
        + text(plan.getCurrentBlocker());
  }

  /** Congela as evidencias comerciais disponiveis no momento da solicitacao. */
  private String buildEvidenceSnapshot(CommercialPlan plan) {
    return buildEvidenceSnapshot(plan, List.of());
  }

  /** Congela o plano e a memoria consolidada para impedir ciclos repetitivos sem aprendizado. */
  private String buildEvidenceSnapshot(
      CommercialPlan plan, List<GrowthOperatorExecution> executionHistory) {
    LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("planId", plan.getId());
    snapshot.put("objective", plan.getCommercialObjective());
    snapshot.put("blocker", plan.getCurrentBlocker());
    snapshot.put("rootCause", plan.getRootCause());
    snapshot.put("nextAction", plan.getNextAction());
    snapshot.put("successCriteria", plan.getSuccessCriteria());
    snapshot.put("stopCriteria", plan.getStopCriteria());
    snapshot.put("maxBudget", plan.getMaxBudget());
    snapshot.put("actualCost", plan.getActualTotalCost());
    snapshot.put("actualRevenue", plan.getActualRevenue());
    snapshot.put("deadline", plan.getDeadline());
    if (plan.getExperiment() != null) {
      Long experimentId = plan.getExperiment().getId();
      snapshot.put("experimentId", experimentId);
      snapshot.put("sessionIntelligence", sessionIntelligence(plan.getId(), SESSION_EVENT_LIMIT));
      snapshot.put("videoStrategyIntelligence", videoStrategyIntelligence(plan.getId()));
    } else {
      snapshot.put(
          "sessionIntelligence", Map.of("available", false, "reason", "PLAN_WITHOUT_EXPERIMENT"));
    }
    snapshot.put("consolidatedMemory", buildConsolidatedMemory(executionHistory));
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha no modulo growth-operator ao congelar evidencias do planId={}", plan.getId(), ex);
      throw new IllegalStateException("Falha ao serializar evidencias do planejamento", ex);
    }
  }

  /** Consolida todo o historico em contagens e preserva uma linha do tempo recente e relevante. */
  private Map<String, Object> buildConsolidatedMemory(
      List<GrowthOperatorExecution> executionHistory) {
    LinkedHashMap<String, Object> memory = new LinkedHashMap<>();
    memory.put("totalCycles", executionHistory.size());
    memory.put(
        "completedCycles",
        executionHistory.stream()
            .filter(item -> item.getStatus() == GrowthOperatorExecutionStatus.COMPLETED)
            .count());
    memory.put(
        "failedCycles",
        executionHistory.stream()
            .filter(item -> item.getStatus() == GrowthOperatorExecutionStatus.FAILED)
            .count());
    memory.put("timelineLimit", MEMORY_TIMELINE_LIMIT);
    memory.put("timelineTruncated", executionHistory.size() > MEMORY_TIMELINE_LIMIT);
    memory.put(
        "timeline",
        executionHistory.stream().limit(MEMORY_TIMELINE_LIMIT).map(this::toMemoryItem).toList());
    return memory;
  }

  /** Resume um ciclo sem afirmar que uma recomendacao foi executada ou gerou venda. */
  private Map<String, Object> toMemoryItem(GrowthOperatorExecution execution) {
    LinkedHashMap<String, Object> item = new LinkedHashMap<>();
    item.put("cycle", execution.getCycleNumber());
    item.put("status", execution.getStatus());
    item.put("createdAt", execution.getCreatedAt());
    item.put("finishedAt", execution.getFinishedAt());
    item.put("decision", execution.getRecommendedDecision());
    item.put("recommendedActionNotConfirmedAsExecuted", execution.getRecommendedAction());
    item.put("conclusion", diagnosisField(execution.getDiagnosisJson(), "rootCause"));
    item.put("evidence", diagnosisField(execution.getDiagnosisJson(), "evidence"));
    item.put("dailyReport", execution.getDailyReport());
    item.put("error", execution.getErrorMessage());
    item.put("observedPlanMetrics", observedPlanMetrics(execution.getEvidenceSnapshot()));
    return item;
  }

  /**
   * Recupera um campo do diagnostico persistido sem interromper o proximo ciclo por JSON legado.
   */
  private Object diagnosisField(String diagnosisJson, String field) {
    if (!hasText(diagnosisJson)) {
      return null;
    }
    try {
      JsonNode value = objectMapper.readTree(diagnosisJson).get(field);
      return value == null || value.isNull()
          ? null
          : objectMapper.convertValue(value, Object.class);
    } catch (JsonProcessingException ex) {
      log.warn("Diagnostico legado invalido ao consolidar memoria, campo={}", field, ex);
      return null;
    }
  }

  /**
   * Recupera metricas observadas no ciclo para comparar recomendacoes com resultados posteriores.
   */
  private Map<String, Object> observedPlanMetrics(String evidenceSnapshot) {
    if (!hasText(evidenceSnapshot)) {
      return Map.of();
    }
    try {
      JsonNode snapshot = objectMapper.readTree(evidenceSnapshot);
      LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
      metrics.put("actualCost", jsonValue(snapshot.get("actualCost")));
      metrics.put("actualRevenue", jsonValue(snapshot.get("actualRevenue")));
      metrics.put("blocker", jsonValue(snapshot.get("blocker")));
      metrics.put("nextAction", jsonValue(snapshot.get("nextAction")));
      return metrics;
    } catch (JsonProcessingException ex) {
      log.warn("Snapshot legado invalido ao consolidar memoria", ex);
      return Map.of();
    }
  }

  /** Converte um valor JSON escalar preservando numero, booleano ou texto. */
  private Object jsonValue(JsonNode value) {
    return value == null || value.isNull() ? null : objectMapper.convertValue(value, Object.class);
  }

  /** Converte a entidade persistida no contrato publico sem expor mutacoes. */
  private GrowthOperatorExecutionResponse toResponse(GrowthOperatorExecution execution) {
    return new GrowthOperatorExecutionResponse(
        execution.getId(),
        execution.getCommercialPlan().getId(),
        execution.getWeekNumber(),
        execution.getStatus(),
        execution.getAuthorityMode(),
        execution.getObjective(),
        execution.getBlocker(),
        execution.getEvidenceSnapshot(),
        execution.getAlternativesJson(),
        execution.getDiagnosisJson(),
        execution.getRecommendedDecision(),
        execution.getRecommendedAction(),
        execution.getDailyReport(),
        execution.getCycleNumber(),
        execution.getAutomaticCycle(),
        execution.getErrorMessage(),
        execution.getModel(),
        execution.getInputTokens(),
        execution.getOutputTokens(),
        execution.getEstimatedCost(),
        execution.getStartedAt(),
        execution.getFinishedAt(),
        execution.getCreatedAt());
  }

  /** Calcula a sequencia auditavel do ciclo dentro do planejamento. */
  private int nextCycleNumber(Long planId) {
    return repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId).stream()
            .map(GrowthOperatorExecution::getCycleNumber)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0)
        + 1;
  }

  /** Indica se um texto possui conteudo util. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Normaliza texto ausente para uso seguro no objetivo. */
  private String text(String value) {
    return hasText(value) ? value : "nao informado";
  }
}

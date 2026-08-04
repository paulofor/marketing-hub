package com.marketinghub.growthoperator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import com.marketinghub.growthoperator.service.result.CompleteGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.result.FailGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.start.StartGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.view.GrowthOperatorExecutionResponse;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
  private final GrowthOperatorExecutionRepository repository;
  private final CommercialPlanService commercialPlanService;
  private final ObjectMapper objectMapper;

  public GrowthOperatorService(
      GrowthOperatorExecutionRepository repository,
      CommercialPlanService commercialPlanService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.commercialPlanService = commercialPlanService;
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
    execution.setEvidenceSnapshot(buildEvidenceSnapshot(plan));
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
    execution.setEvidenceSnapshot(buildEvidenceSnapshot(plan, latest.orElse(null)));
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
    return buildEvidenceSnapshot(plan, null);
  }

  /** Congela o plano e o ultimo aprendizado para impedir ciclos repetitivos sem memoria. */
  private String buildEvidenceSnapshot(
      CommercialPlan plan, GrowthOperatorExecution previousExecution) {
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
    if (previousExecution != null) {
      snapshot.put("previousCycle", previousExecution.getCycleNumber());
      snapshot.put("previousDecision", previousExecution.getRecommendedDecision());
      snapshot.put("previousAction", previousExecution.getRecommendedAction());
      snapshot.put("previousDailyReport", previousExecution.getDailyReport());
    }
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha no modulo growth-operator ao congelar evidencias do planId={}", plan.getId(), ex);
      throw new IllegalStateException("Falha ao serializar evidencias do planejamento", ex);
    }
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

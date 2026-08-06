package com.marketinghub.financialagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: congelar fontes financeiras e auditar conciliacoes somente leitura. */
@Service
public class FinancialAgentService {
  private static final String READ_ONLY = "READ_ONLY_FINANCIAL_RECONCILIATION";
  private final FinancialAgentExecutionRepository repository;
  private final CommercialPlanService commercialPlanService;
  private final ObjectMapper objectMapper;
  private final StudioCostLedgerService studioCostLedgerService;

  public FinancialAgentService(
      FinancialAgentExecutionRepository repository,
      CommercialPlanService commercialPlanService,
      ObjectMapper objectMapper,
      StudioCostLedgerService studioCostLedgerService) {
    this.repository = repository;
    this.commercialPlanService = commercialPlanService;
    this.objectMapper = objectMapper;
    this.studioCostLedgerService = studioCostLedgerService;
  }

  /** Cria uma conciliacao manual com snapshot imutavel das fontes atuais. */
  @Transactional
  public FinancialAgentExecutionResponse start(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.PENDING);
    execution.setAuthorityMode(READ_ONLY);
    execution.setFinancialSnapshot(buildSnapshot(plan));
    return toResponse(repository.save(execution));
  }

  /** Cria no maximo uma conciliacao automatica por dia para o planejamento. */
  @Transactional
  public FinancialAgentExecutionResponse ensureDaily(Long planId) {
    List<FinancialAgentExecution> history =
        repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId);
    if (!history.isEmpty()) {
      LocalDate lastDate = history.getFirst().getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
      if (lastDate.equals(LocalDate.now(ZoneOffset.UTC))) {
        return toResponse(history.getFirst());
      }
    }
    return start(planId);
  }

  /** Lista relatorios financeiros diarios do planejamento. */
  @Transactional(readOnly = true)
  public List<FinancialAgentExecutionResponse> list(Long planId) {
    commercialPlanService.getPlan(planId);
    return repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Entrega o snapshot financeiro atual para consultas oficiais somente leitura. */
  @Transactional(readOnly = true)
  public Map<String, Object> intelligence(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    try {
      return objectMapper.readValue(buildSnapshot(plan), Map.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Snapshot financeiro invalido.", ex);
    }
  }

  /** Reserva uma unica conciliacao pendente para o executor externo. */
  @Transactional
  public FinancialAgentExecutionResponse claimPending() {
    List<FinancialAgentExecution> pending =
        repository.findByStatusOrderByCreatedAtAsc(
            FinancialAgentExecutionStatus.PENDING, PageRequest.of(0, 1));
    if (pending.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma conciliacao pendente.");
    }
    FinancialAgentExecution execution = pending.getFirst();
    execution.setStatus(FinancialAgentExecutionStatus.RUNNING);
    execution.setStartedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Persiste o relatorio sem executar qualquer decisao financeira. */
  @Transactional
  public FinancialAgentExecutionResponse complete(Long id, CompleteFinancialAgentRequest request) {
    FinancialAgentExecution execution = findRunning(id);
    if (request == null
        || !hasText(request.dailyReport())
        || !hasText(request.reconciliationJson())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conciliacao incompleta.");
    }
    execution.setReconciliationJson(request.reconciliationJson());
    execution.setDailyReport(request.dailyReport());
    execution.setRawModelResponse(request.rawModelResponse());
    execution.setModel(request.model());
    execution.setEstimatedCost(request.estimatedCost());
    execution.setStatus(FinancialAgentExecutionStatus.COMPLETED);
    execution.setFinishedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Registra falha tecnica preservando o snapshot que a originou. */
  @Transactional
  public FinancialAgentExecutionResponse fail(Long id, FailFinancialAgentRequest request) {
    FinancialAgentExecution execution = findRunning(id);
    execution.setStatus(FinancialAgentExecutionStatus.FAILED);
    execution.setErrorMessage(request == null ? "Falha nao informada." : request.errorMessage());
    execution.setFinishedAt(Instant.now());
    return toResponse(repository.save(execution));
  }

  /** Consolida planejamento, campanha, IA, demais provedores, receita e cobertura das fontes. */
  private String buildSnapshot(CommercialPlan plan) {
    BigDecimal campaign = money(plan.getActualCampaignCost());
    BigDecimal ai = money(plan.getActualAiCost());
    BigDecimal total = money(plan.getActualTotalCost());
    BigDecimal other = total.subtract(campaign).subtract(ai).max(BigDecimal.ZERO);
    BigDecimal revenue = money(plan.getActualRevenue());
    LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("capturedAt", Instant.now());
    snapshot.put("planId", plan.getId());
    snapshot.put("planName", plan.getName());
    snapshot.put("periodStart", plan.getCreatedAt());
    snapshot.put("periodEnd", plan.getDeadline());
    snapshot.put("monthlyBudgetCeilingBrl", plan.getMaxBudget());
    snapshot.put("campaignCostBrl", campaign);
    snapshot.put("aiProviderCostBrl", ai);
    snapshot.put("otherAttributedCostBrl", other);
    snapshot.put("totalCostBrl", total);
    snapshot.put("approvedRevenueBrl", revenue);
    snapshot.put("studioKnownCostUsd", studioCostLedgerService.totalKnownCostUsd(plan.getId()));
    snapshot.put("studioCostCoverage", studioCostLedgerService.coverage(plan.getId()));
    snapshot.put(
        "studioProviderEfficiency", studioCostLedgerService.providerEfficiency(plan.getId()));
    snapshot.put("studioUnassignedKnownCostUsd", studioCostLedgerService.totalUnassignedCostUsd());
    snapshot.put("studioUnassignedCostCoverage", studioCostLedgerService.unassignedCoverage());
    snapshot.put(
        "studioCostInterpretation",
        "Custo conhecido igual a zero nao comprova custo real zero quando a cobertura estiver vazia ou parcial. Tentativas sem plano ficam separadas e bloqueiam conclusao ate serem atribuidas.");
    snapshot.put("contributionBeforeRefundsBrl", revenue.subtract(total));
    snapshot.put("refundsBrl", null);
    snapshot.put(
        "experimentId", plan.getExperiment() == null ? null : plan.getExperiment().getId());
    snapshot.put(
        "sourceCoverage",
        Map.of(
            "campaigns", "CONSOLIDATED_IN_COMMERCIAL_PLAN",
            "aiAndVideoProviders", "STUDIO_LEDGER_REQUIRES_NON_EMPTY_COMPLETE_COVERAGE",
            "approvedSales", "CONSOLIDATED_IN_COMMERCIAL_PLAN",
            "refunds", "NOT_YET_AVAILABLE_AS_SEPARATE_SOURCE",
            "infrastructure", "NOT_YET_ATTRIBUTED_BY_PLAN"));
    snapshot.put(
        "guardrails",
        List.of(
            "Nao movimentar dinheiro ou comprar creditos.",
            "Nao alterar preco, orcamento ou campanha.",
            "Nao tratar projecao, pedido ou impacto estimado como receita.",
            "Bloquear conclusao quando fontes divergirem ou estiverem ausentes."));
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Nao foi possivel congelar o snapshot financeiro.", ex);
    }
  }

  /** Busca uma execucao que esteja legitimamente em processamento. */
  private FinancialAgentExecution findRunning(Long id) {
    FinancialAgentExecution execution =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (execution.getStatus() != FinancialAgentExecutionStatus.RUNNING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Conciliacao fora de execucao.");
    }
    return execution;
  }

  /** Converte valor ausente em zero sem criar receita ou custo estimado. */
  private BigDecimal money(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Verifica se o texto possui conteudo auditavel. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Converte a entidade persistida no contrato da API. */
  private FinancialAgentExecutionResponse toResponse(FinancialAgentExecution execution) {
    return new FinancialAgentExecutionResponse(
        execution.getId(),
        execution.getCommercialPlan().getId(),
        execution.getStatus(),
        execution.getAuthorityMode(),
        execution.getFinancialSnapshot(),
        execution.getReconciliationJson(),
        execution.getDailyReport(),
        execution.getModel(),
        execution.getErrorMessage(),
        execution.getStartedAt(),
        execution.getFinishedAt(),
        execution.getCreatedAt());
  }
}

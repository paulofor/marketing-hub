package com.marketinghub.financialagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskBlockerGuidanceRequest;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskHelpLinkRequest;
import com.marketinghub.agenttask.AgentTaskModelUsageRequest;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.agenttask.FailAgentTaskRequest;
import com.marketinghub.agenttask.UpdateAgentTaskStatusRequest;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: congelar fontes financeiras e auditar conciliacoes somente leitura. */
@Service
public class FinancialAgentService {
  private static final Logger log = LoggerFactory.getLogger(FinancialAgentService.class);
  private static final String READ_ONLY = "READ_ONLY_FINANCIAL_RECONCILIATION";
  private static final String REVENUE_PROJECTION = "READ_ONLY_REVENUE_PROJECTION";
  private static final String ASSUMPTION_DEFINITION = "COMMERCIAL_ASSUMPTIONS_VALIDATION";
  private final FinancialAgentExecutionRepository repository;
  private final CommercialPlanService commercialPlanService;
  private final ObjectMapper objectMapper;
  private final StudioCostLedgerService studioCostLedgerService;
  private final CommercialPlanVersionService versionService;
  private final AgentTaskService taskService;

  /** Configura fontes financeiras, versão comercial e integração com a mesa de Plutus. */
  @Autowired
  public FinancialAgentService(
      FinancialAgentExecutionRepository repository,
      CommercialPlanService commercialPlanService,
      ObjectMapper objectMapper,
      StudioCostLedgerService studioCostLedgerService,
      CommercialPlanVersionService versionService,
      AgentTaskService taskService) {
    this.repository = repository;
    this.commercialPlanService = commercialPlanService;
    this.objectMapper = objectMapper;
    this.studioCostLedgerService = studioCostLedgerService;
    this.versionService = versionService;
    this.taskService = taskService;
  }

  /** Mantém construção direta dos testes legados de conciliação. */
  FinancialAgentService(
      FinancialAgentExecutionRepository repository,
      CommercialPlanService commercialPlanService,
      ObjectMapper objectMapper,
      StudioCostLedgerService studioCostLedgerService) {
    this(repository, commercialPlanService, objectMapper, studioCostLedgerService, null, null);
  }

  /** Cria uma conciliacao manual com snapshot imutavel das fontes atuais. */
  @Transactional
  public FinancialAgentExecutionResponse start(Long planId) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.PENDING);
    execution.setAuthorityMode(READ_ONLY);
    execution.setCommercialPlanVersion(currentVersion(planId));
    execution.setFinancialSnapshot(buildSnapshot(plan));
    return toResponse(repository.save(execution));
  }

  /** Abre uma projeção tipada e uma tarefa correlacionada na mesa de Plutus. */
  @Transactional
  public FinancialAgentExecutionResponse startRevenueProjection(
      Long planId, StartRevenueProjectionRequest request) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    int version = currentVersion(planId);
    String context = request == null ? null : trimToNull(request.decisionContext());
    AgentTaskResponse task =
        taskService.createByHuman(
            new CreateAgentTaskRequest(
                "financial-agent",
                "Plano Comercial",
                "Estimar receita e investimento de " + plan.getName(),
                "Produzir cenários conservador, base e otimista, com premissas, margem, CAC, ROAS, ponto de equilíbrio, teto recomendado e critérios de continuar, ajustar ou parar."
                    + (context == null ? "" : " Contexto de decisão: " + context),
                "HIGH",
                "commercial-plan:" + planId + "@v" + version + ":revenue-projection",
                null,
                null,
                true,
                "Automação financeira ainda não cadastrada como processo BPM."));
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.PENDING);
    execution.setAuthorityMode(REVENUE_PROJECTION);
    execution.setCommercialPlanVersion(version);
    execution.setAgentTaskId(task.id());
    execution.setProjectionRequest(context);
    execution.setFinancialSnapshot(buildSnapshot(plan));
    return toResponse(repository.save(execution));
  }

  /** Enfileira Plutus para validar financeiramente a proposta produzida por Atena. */
  @Transactional
  public FinancialAgentExecutionResponse startAssumptionValidation(
      Long planId, Long strategistExecutionId, String atenaProposal) {
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    int version = currentVersion(planId);
    AgentTaskResponse task =
        taskService.createByHuman(
            new CreateAgentTaskRequest(
                "financial-agent",
                "Plano Comercial",
                "Validar premissas propostas para " + plan.getName(),
                "Revisar coerência, margem, ponto de equilíbrio e risco das hipóteses propostas por Atena, sem autorizar gasto.",
                "HIGH",
                "commercial-plan:"
                    + planId
                    + "@v"
                    + version
                    + ":assumptions:"
                    + strategistExecutionId,
                null,
                null,
                true,
                "Automação financeira ainda não cadastrada como processo BPM."));
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.PENDING);
    execution.setAuthorityMode(ASSUMPTION_DEFINITION);
    execution.setCommercialPlanVersion(version);
    execution.setAgentTaskId(task.id());
    execution.setProjectionRequest(atenaProposal);
    execution.setFinancialSnapshot(buildSnapshot(plan));
    return toResponse(repository.save(execution));
  }

  /** Lista as definições conjuntas sem misturá-las às projeções de receita. */
  @Transactional(readOnly = true)
  public List<FinancialAgentExecutionResponse> listAssumptionDefinitions(Long planId) {
    commercialPlanService.getPlan(planId);
    return repository
        .findByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDesc(planId, ASSUMPTION_DEFINITION)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  /** Lista somente projeções, mantendo-as distintas de receita realizada e conciliações. */
  @Transactional(readOnly = true)
  public List<FinancialAgentExecutionResponse> listRevenueProjections(Long planId) {
    commercialPlanService.getPlan(planId);
    return repository
        .findByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDesc(planId, REVENUE_PROJECTION)
        .stream()
        .map(this::toResponse)
        .toList();
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
      log.error("Falha ao desserializar snapshot financeiro. planId={}", planId, ex);
      throw new IllegalStateException("Snapshot financeiro invalido.", ex);
    }
  }

  /**
   * Entrega a Plutus o contexto seguro de custos quando o projeto legado ainda não possui plano.
   */
  @Transactional(readOnly = true)
  public Map<String, Object> unassignedStudioIntelligence(Long productId) {
    LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("capturedAt", Instant.now());
    snapshot.put("productId", productId);
    snapshot.put("commercialPlanId", null);
    snapshot.put("studioUnassignedKnownCostUsd", studioCostLedgerService.totalUnassignedCostUsd());
    snapshot.put("studioUnassignedCostCoverage", studioCostLedgerService.unassignedCoverage());
    snapshot.put("decisionScope", "VIDEO_CYCLE_BUDGET_ONLY");
    snapshot.put(
        "guardrails",
        List.of(
            "Nao movimentar dinheiro ou comprar creditos.",
            "Nao aprovar consumo acima do teto do ciclo.",
            "Bloquear quando custos estiverem ausentes ou divergentes."));
    return snapshot;
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
    if (execution.getAgentTaskId() != null) {
      taskService.updateStatus(
          execution.getAgentTaskId(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    }
    return toResponse(repository.save(execution));
  }

  /** Entrega ao MCP o snapshot congelado da conciliacao reservada. */
  @Transactional(readOnly = true)
  public FinancialAgentExecutionResponse getExecution(Long id) {
    return toResponse(findRunning(id));
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
    validateResultJson(id, request.reconciliationJson());
    execution.setReconciliationJson(request.reconciliationJson());
    execution.setDailyReport(request.dailyReport());
    execution.setRawModelResponse(request.rawModelResponse());
    execution.setModel(request.model());
    execution.setEstimatedCost(request.estimatedCost());
    execution.setStatus(FinancialAgentExecutionStatus.COMPLETED);
    execution.setFinishedAt(Instant.now());
    if (ASSUMPTION_DEFINITION.equals(execution.getAuthorityMode())) {
      commercialPlanService.applyAgentAssumptions(
          execution.getCommercialPlan().getId(), request.reconciliationJson());
    }
    FinancialAgentExecution saved = repository.save(execution);
    if (execution.getAgentTaskId() != null) {
      taskService.completeClaimedProcessTask(
          "financial-agent", execution.getAgentTaskId(), taskCompletion(saved, request));
    }
    return toResponse(saved);
  }

  /** Registra falha tecnica preservando o snapshot que a originou. */
  @Transactional
  public FinancialAgentExecutionResponse fail(Long id, FailFinancialAgentRequest request) {
    FinancialAgentExecution execution = findRunning(id);
    String error =
        request == null || !hasText(request.errorMessage())
            ? "Falha nao informada."
            : request.errorMessage().trim();
    execution.setStatus(FinancialAgentExecutionStatus.FAILED);
    execution.setErrorMessage(error);
    execution.setFinishedAt(Instant.now());
    FinancialAgentExecution saved = repository.save(execution);
    if (execution.getAgentTaskId() != null) {
      taskService.failClaimedProcessTask(
          "financial-agent",
          execution.getAgentTaskId(),
          new FailAgentTaskRequest(
              error,
              null,
              failureEvidence(saved, error),
              null,
              null,
              new AgentTaskBlockerGuidanceRequest(
                  "TECHNICAL_FAILURE",
                  "Corrija a causa técnica e reinicie a tarefa de Plutus: " + error,
                  List.of(
                      new AgentTaskHelpLinkRequest("Abrir tarefas dos agentes", "/agent-tasks")))));
    }
    return toResponse(saved);
  }

  /** Monta o callback completo da tarefa sem transformar ausência de telemetria em zero. */
  private CompleteAgentTaskRequest taskCompletion(
      FinancialAgentExecution execution, CompleteFinancialAgentRequest request) {
    return new CompleteAgentTaskRequest(
        request.reconciliationJson(),
        completionEvidence(execution, request),
        modelUsages(request),
        executionAudit(request));
  }

  /** Registra a execução financeira como artefato auditável, separado do resultado funcional. */
  private String completionEvidence(
      FinancialAgentExecution execution, CompleteFinancialAgentRequest request) {
    LinkedHashMap<String, Object> evidence = baseTaskEvidence(execution);
    evidence.put("dailyReport", request.dailyReport());
    evidence.put("model", trimToNull(request.model()));
    evidence.put("reasoningEffort", trimToNull(request.reasoningEffort()));
    evidence.put("requestedServiceTier", trimToNull(request.requestedServiceTier()));
    evidence.put("effectiveServiceTier", trimToNull(request.effectiveServiceTier()));
    evidence.put("serviceTierExceptionReason", trimToNull(request.serviceTierExceptionReason()));
    evidence.put("rawModelResponsePresent", hasText(request.rawModelResponse()));
    evidence.put(
        "rawModelResponseReference",
        hasText(request.rawModelResponse())
            ? "financial_agent_execution:" + execution.getId() + ":raw_model_response"
            : null);
    evidence.put(
        "rawModelResponseSha256",
        hasText(request.rawModelResponse()) ? sha256(request.rawModelResponse()) : null);
    evidence.put("completedAt", execution.getFinishedAt());
    return writeEvidence(execution.getId(), evidence);
  }

  /** Monta evidência mínima da falha técnica sem inventar resposta ou uso de modelo. */
  private String failureEvidence(FinancialAgentExecution execution, String error) {
    LinkedHashMap<String, Object> evidence = baseTaskEvidence(execution);
    evidence.put("status", "FAILED");
    evidence.put("error", error);
    evidence.put("finishedAt", execution.getFinishedAt());
    return writeEvidence(execution.getId(), evidence);
  }

  /** Monta o resultado funcional de falha separado da evidência técnica. */
  private String failureResult(FinancialAgentExecution execution, String error) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("status", "FAILED");
    result.put("executionId", execution.getId());
    result.put("authorityMode", execution.getAuthorityMode());
    result.put("error", error);
    return writeEvidence(execution.getId(), result);
  }

  /** Reúne a identidade imutável da execução e confirma que não houve efeito comercial externo. */
  private LinkedHashMap<String, Object> baseTaskEvidence(FinancialAgentExecution execution) {
    LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("artifactType", "FINANCIAL_AGENT_EXECUTION");
    evidence.put("executionId", execution.getId());
    evidence.put("authorityMode", execution.getAuthorityMode());
    evidence.put("commercialPlanId", execution.getCommercialPlan().getId());
    evidence.put("commercialPlanVersion", execution.getCommercialPlanVersion());
    evidence.put("financialSnapshotSha256", sha256(execution.getFinancialSnapshot()));
    evidence.put("externalSideEffects", false);
    evidence.put("publicationPerformed", false);
    evidence.put("spendAuthorized", false);
    return evidence;
  }

  /** Converte somente contadores completos em consumo de modelo estimável pelo catálogo. */
  private List<AgentTaskModelUsageRequest> modelUsages(CompleteFinancialAgentRequest request) {
    boolean anyToken =
        request.inputTokens() != null
            || request.cachedInputTokens() != null
            || request.outputTokens() != null;
    if (!anyToken) return null;
    if (request.inputTokens() == null
        || request.cachedInputTokens() == null
        || request.outputTokens() == null
        || !hasText(request.model())
        || !hasText(request.effectiveServiceTier())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Telemetria financeira de modelo incompleta.");
    }
    return List.of(
        new AgentTaskModelUsageRequest(
            request.model().trim(),
            normalizedServiceTier(request.effectiveServiceTier()),
            request.inputTokens(),
            request.cachedInputTokens(),
            request.outputTokens()));
  }

  /** Exige e preserva modelo, esforço e prompt integral em toda conclusão financeira. */
  private AgentTaskExecutionAuditRequest executionAudit(CompleteFinancialAgentRequest request) {
    if (!hasText(request.model())
        || !hasText(request.reasoningEffort())
        || !hasText(request.promptSent())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Conclusão financeira exige modelo, tipo de raciocínio e prompt integral.");
    }
    return new AgentTaskExecutionAuditRequest(
        request.model().trim(), request.reasoningEffort().trim(), request.promptSent());
  }

  /** Normaliza o tier efetivo para o vocabulário de precificação do backend. */
  private String normalizedServiceTier(String tier) {
    String normalized = tier == null ? "" : tier.trim().toUpperCase();
    if ("DEFAULT".equals(normalized)) return "STANDARD";
    if (List.of("STANDARD", "FLEX", "BATCH").contains(normalized)) return normalized;
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tier financeiro inválido.");
  }

  /** Rejeita resultado que não seja um objeto JSON funcional. */
  private void validateResultJson(Long executionId, String resultJson) {
    try {
      JsonNode result = objectMapper.readTree(resultJson);
      if (result == null || !result.isObject()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Resultado financeiro deve ser um objeto JSON.");
      }
    } catch (JsonProcessingException ex) {
      log.error("Falha ao validar resultado financeiro. executionId={}", executionId, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Resultado financeiro não contém JSON válido.", ex);
    }
  }

  /** Serializa evidência estruturada com correlação explícita da execução. */
  private String writeEvidence(Long executionId, Map<String, Object> evidence) {
    try {
      return objectMapper.writeValueAsString(evidence);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar evidência financeira. executionId={}", executionId, ex);
      throw new IllegalStateException("Não foi possível persistir a evidência financeira.", ex);
    }
  }

  /** Calcula a assinatura SHA-256 sem expor o conteúdo sensível no histórico. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível ao auditar execução financeira.", ex);
      throw new IllegalStateException("Não foi possível assinar a evidência financeira.", ex);
    }
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
    snapshot.put("offerPriceBrl", plan.getOfferPriceBrl());
    snapshot.put("variableCostPerSaleBrl", plan.getVariableCostPerSaleBrl());
    snapshot.put("expectedMonthlyTraffic", plan.getExpectedMonthlyTraffic());
    snapshot.put("expectedConversionRatePercent", plan.getExpectedConversionRatePercent());
    snapshot.put("expectedCacBrl", plan.getExpectedCacBrl());
    snapshot.put("expectedRefundRatePercent", plan.getExpectedRefundRatePercent());
    snapshot.put("fixedOperationalCostBrl", plan.getFixedOperationalCostBrl());
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
      log.error("Falha ao congelar snapshot financeiro. planId={}", plan.getId(), ex);
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

  /** Resolve a versão oficial congelada para a execução financeira. */
  private int currentVersion(Long planId) {
    return versionService == null ? 1 : versionService.current(planId).versionNumber();
  }

  /** Normaliza o contexto opcional da decisão. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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
        execution.getCommercialPlanVersion(),
        execution.getAgentTaskId(),
        execution.getProjectionRequest(),
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

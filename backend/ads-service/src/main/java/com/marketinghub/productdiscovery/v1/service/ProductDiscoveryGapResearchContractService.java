package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: validar planos, consumo e evidências da pesquisa dirigida por lacunas de Argos.
 */
@Service
public class ProductDiscoveryGapResearchContractService {
  public static final BigDecimal ESTIMATED_SEARCH_COST_PER_REQUEST_USD =
      new BigDecimal("0.00500000");
  private static final String AUTHORIZED_FOR_COLLECTION = "AUTHORIZED_FOR_COLLECTION";
  private static final String REJECTED_REPEATED_RESEARCH_LENS = "REJECTED_REPEATED_RESEARCH_LENS";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProductDiscoveryGapResearchContractService.class);

  private final ProductDiscoveryOpportunityRepository opportunityRepository;

  /** Inicializa o contrato com a fonte canônica das candidatas preservadas. */
  public ProductDiscoveryGapResearchContractService(
      ProductDiscoveryOpportunityRepository opportunityRepository) {
    this.opportunityRepository = opportunityRepository;
  }

  /** Expõe ao worker a mesma política aplicada nos callbacks do backend. */
  public ProductDiscoveryGapResearchPolicyResponse policy() {
    return new ProductDiscoveryGapResearchPolicyResponse(
        ProductDiscoveryCustomerInterviewService.MAXIMUM_DEEPENING_ATTEMPTS,
        ProductDiscoveryCustomerInterviewService.MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT,
        ProductDiscoveryCustomerInterviewService.MAXIMUM_MODEL_INVOCATIONS,
        ESTIMATED_SEARCH_COST_PER_REQUEST_USD,
        ProductDiscoveryCustomerInterviewService.MAXIMUM_SEARCH_COST_USD,
        "ESTIMATED_SEARCH_ONLY",
        "AGENT_TASK_AUDIT_AFTER_CALLBACK",
        ProductDiscoveryCustomerInterviewService.SEARCH_PRICING_SOURCE,
        ProductDiscoveryCustomerInterviewService.SEARCH_PRICING_OBSERVED_ON);
  }

  /** Exige uma pergunta, fonte, evidência, contraponto e limite para cada lacuna planejada. */
  public void validatePlan(ProductDiscoveryCycle cycle, String planJson) {
    try {
      JsonNode root = JSON.readTree(planJson);
      List<GapPlanAttempt> attempts = new java.util.ArrayList<>();
      if (root.path("attempts").isArray()) {
        if (root.path("attempts").isEmpty()
            || root.path("attempts").size()
                > ProductDiscoveryCustomerInterviewService.MAXIMUM_DEEPENING_ATTEMPTS) {
          throw new IllegalArgumentException("Histórico de tentativas fora do limite");
        }
        int expectedAttempt = 1;
        for (JsonNode attempt : root.path("attempts")) {
          if (attempt.path("attemptNumber").asInt(-1) != expectedAttempt
              || !attempt.path("plan").isObject()) {
            throw new IllegalArgumentException("Histórico de tentativas inválido");
          }
          String disposition = attempt.path("planDisposition").asText(AUTHORIZED_FOR_COLLECTION);
          boolean rejectedRepeatedLens = REJECTED_REPEATED_RESEARCH_LENS.equals(disposition);
          if ((!AUTHORIZED_FOR_COLLECTION.equals(disposition) && !rejectedRepeatedLens)
              || (rejectedRepeatedLens
                  && (expectedAttempt == 1 || expectedAttempt != root.path("attempts").size()))) {
            throw new IllegalArgumentException("Disposição de tentativa inválida");
          }
          attempts.add(new GapPlanAttempt(attempt.path("plan"), rejectedRepeatedLens));
          expectedAttempt++;
        }
      } else {
        attempts.add(new GapPlanAttempt(root, false));
      }

      Set<String> executedQueries = new HashSet<>();
      Set<String> executedResearchLenses = new HashSet<>();
      BigDecimal executedCost = BigDecimal.ZERO;
      for (int index = 0; index < attempts.size(); index++) {
        GapPlanAttempt attempt = attempts.get(index);
        JsonNode plan = attempt.plan();
        String researchLens = normalizeIdentity(plan.path("researchLens").asText());
        if (!StringUtils.hasText(researchLens)) {
          throw new IllegalArgumentException("Tentativa sem lente de pesquisa");
        }
        GapPlanUsage usage = validateAttemptPlan(cycle, plan);
        if (attempt.rejectedRepeatedLens()) continue;
        if (!executedResearchLenses.add(researchLens)
            || (index > 0 && "INITIAL_SCOPE".equals(plan.path("expansionAxis").asText()))) {
          throw new IllegalArgumentException("Tentativas repetem lente ou escopo inicial");
        }
        if (!java.util.Collections.disjoint(executedQueries, usage.queries())) {
          throw new IllegalArgumentException("Tentativas repetem consultas já executadas");
        }
        executedQueries.addAll(usage.queries());
        executedCost = executedCost.add(usage.estimatedCost());
      }
      int maximumTotalQueries =
          ProductDiscoveryCustomerInterviewService.MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT
              * ProductDiscoveryCustomerInterviewService.MAXIMUM_DEEPENING_ATTEMPTS;
      if (executedQueries.size() > maximumTotalQueries
          || executedCost.compareTo(
                  ProductDiscoveryCustomerInterviewService.MAXIMUM_SEARCH_COST_USD)
              > 0) {
        throw new IllegalArgumentException("Histórico excede o teto total de busca");
      }
    } catch (Exception ex) {
      LOGGER.error(
          "[product-discovery] Plano de aprofundamento inválido cycleId={}", cycle.getId(), ex);
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Plano de aprofundamento não comprova perguntas, fontes e limites por candidata",
          ex);
    }
  }

  /** Valida que o relatório final prova limites, política de evidências e lacunas tratadas. */
  public void validateEvidenceReport(
      ProductDiscoveryCycle cycle,
      ProductDiscoveryResultRequest request,
      ProductDiscoveryGapDeepeningResponse gate) {
    if (request.evidenceReport() == null) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O aprofundamento deve entregar um relatório auditável de lacunas e consumo");
    }
    if (cycle.usesPublicEvidence()) {
      if (!cycle
          .getEvidencePolicy()
          .equals(request.evidenceReport().path("gapDeepening").path("evidencePolicy").asText())) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "A política do relatório diverge do ciclo");
      }
      request
          .opportunities()
          .forEach(
              candidate ->
                  ProductDiscoveryPublicEvidenceContract.validate(cycle.getId(), candidate));
    }
    JsonNode deepening = request.evidenceReport().path("gapDeepening");
    Set<String> expectedCandidates =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId()).stream()
            .map(item -> normalizeIdentity(item.getName()))
            .collect(Collectors.toSet());
    Set<String> reportedCandidates = new HashSet<>();
    Set<String> reportedQueries = new HashSet<>();
    Map<String, Set<String>> reportedQueriesByCandidate = new LinkedHashMap<>();
    JsonNode resolvedGaps = deepening.path("resolvedGaps");
    if (resolvedGaps.isArray()) {
      for (JsonNode gap : resolvedGaps) {
        requireReportText(gap, "candidateName");
        requireReportText(gap, "pendingQuestion");
        requireReportText(gap, "appropriateSource");
        requireReportText(gap, "evidenceNeeded");
        requireReportText(gap, "contraryEvidenceSought");
        requireReportText(gap, "status");
        String status = gap.path("status").asText();
        String candidateName = normalizeIdentity(gap.path("candidateName").asText());
        if (!Set.of("RESOLVED", "CONTRADICTED", "STILL_OPEN").contains(status)
            || !reportedCandidates.add(candidateName)) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "O relatório de aprofundamento repetiu candidata ou informou estado inválido");
        }
        JsonNode queries = gap.path("executedQueries");
        if (!queries.isArray() || queries.isEmpty()) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "Cada lacuna deve registrar as consultas efetivamente executadas");
        }
        Set<String> candidateQueries = jsonTextSet(queries);
        if (candidateQueries.isEmpty() || candidateQueries.size() != queries.size()) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "O relatório contém consulta de aprofundamento vazia ou repetida");
        }
        if (candidateQueries.stream().anyMatch(query -> !reportedQueries.add(query))) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "Uma consulta não pode ser atribuída a mais de uma candidata");
        }
        validateGapResolution(gap, candidateQueries, status);
        reportedQueriesByCandidate.put(candidateName, candidateQueries);
      }
    }
    validateReportEnvelope(
        cycle,
        deepening,
        gate,
        expectedCandidates,
        reportedCandidates,
        reportedQueries,
        reportedQueriesByCandidate);
  }

  /** Confere a prova específica usada para resolver, contradizer ou manter aberta cada lacuna. */
  private void validateGapResolution(JsonNode gap, Set<String> candidateQueries, String status) {
    Set<String> resolutionQueries = jsonTextSetAllowEmpty(gap.path("resolutionQueries"));
    Set<String> resolutionEvidenceIds = jsonTextSetAllowEmpty(gap.path("resolutionEvidenceIds"));
    String resolutionBasis = gap.path("resolutionBasis").asText();
    if (!candidateQueries.containsAll(resolutionQueries)
        || !Set.of("NEW_PUBLIC_SEARCH", "INTERVIEWS_OR_REUSED_EVIDENCE", "NO_RESOLUTION_EVIDENCE")
            .contains(resolutionBasis)
        || ("NEW_PUBLIC_SEARCH".equals(resolutionBasis) && resolutionQueries.isEmpty())
        || ("INTERVIEWS_OR_REUSED_EVIDENCE".equals(resolutionBasis)
            && resolutionEvidenceIds.isEmpty())
        || (Set.of("RESOLVED", "CONTRADICTED").contains(status)
            && resolutionEvidenceIds.isEmpty())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Cada conclusão deve indicar quais buscas e evidências realmente resolveram a lacuna");
    }
  }

  /** Confere a relação exata entre plano, consultas tentadas, custo e candidatas persistidas. */
  private void validateReportEnvelope(
      ProductDiscoveryCycle cycle,
      JsonNode deepening,
      ProductDiscoveryGapDeepeningResponse gate,
      Set<String> expectedCandidates,
      Set<String> reportedCandidates,
      Set<String> reportedQueries,
      Map<String, Set<String>> reportedQueriesByCandidate) {
    Set<String> executedQueries = jsonTextSet(deepening.path("executedQueries"));
    Set<String> unexecutedQueries = jsonTextSetAllowEmpty(deepening.path("unexecutedQueries"));
    Map<String, Set<String>> plannedQueriesByCandidate = planQueriesByCandidate(cycle);
    Set<String> plannedQueries =
        plannedQueriesByCandidate.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    boolean reportedQueriesRespectPlan =
        reportedQueriesByCandidate.entrySet().stream()
            .allMatch(
                entry ->
                    plannedQueriesByCandidate.containsKey(entry.getKey())
                        && plannedQueriesByCandidate
                            .get(entry.getKey())
                            .containsAll(entry.getValue()));
    Set<String> expectedUnexecutedQueries = new HashSet<>(plannedQueries);
    expectedUnexecutedQueries.removeAll(executedQueries);
    if (!deepening.isObject()
        || !deepening.path("resolvedGaps").isArray()
        || deepening.path("resolvedGaps").isEmpty()
        || deepening.path("customerInterviewCount").asInt(-1) != gate.interviewCount()
        || deepening.path("candidateCount").asInt(-1) != expectedCandidates.size()
        || !reportedCandidates.equals(expectedCandidates)
        || !reportedQueriesRespectPlan
        || !reportedQueries.equals(executedQueries)
        || !unexecutedQueries.equals(expectedUnexecutedQueries)
        || deepening.path("plannedSearchRequests").asInt(-1) != plannedQueries.size()
        || !"ESTIMATED_SEARCH_ONLY".equals(deepening.path("searchCostCoverage").asText())
        || !ProductDiscoveryCustomerInterviewService.SEARCH_PRICING_SOURCE.equals(
            deepening.path("pricingSource").asText())
        || !ProductDiscoveryCustomerInterviewService.SEARCH_PRICING_OBSERVED_ON
            .toString()
            .equals(deepening.path("pricingObservedOn").asText())
        || !"AGENT_TASK_AUDIT_AFTER_CALLBACK"
            .equals(deepening.path("modelCostCoverage").asText())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O relatório de aprofundamento deve vincular fontes e lacunas resolvidas ou ainda pendentes");
    }
    validateReportConsumption(deepening, executedQueries);
  }

  /**
   * Comprova que somente consultas executadas entram na estimativa e que nenhum teto foi excedido.
   */
  private void validateReportConsumption(JsonNode deepening, Set<String> executedQueries) {
    int searchRequests = deepening.path("actualSearchRequests").asInt(-1);
    int modelInvocations = deepening.path("modelInvocationCount").asInt(-1);
    BigDecimal estimatedCost = decimal(deepening.path("estimatedSearchCostUsd"));
    BigDecimal expectedCost =
        ESTIMATED_SEARCH_COST_PER_REQUEST_USD.multiply(BigDecimal.valueOf(searchRequests));
    if (searchRequests < 1
        || searchRequests != executedQueries.size()
        || searchRequests
            > ProductDiscoveryCustomerInterviewService.MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT
                * ProductDiscoveryCustomerInterviewService.MAXIMUM_DEEPENING_ATTEMPTS
        || estimatedCost == null
        || estimatedCost.compareTo(expectedCost) != 0
        || estimatedCost.compareTo(ProductDiscoveryCustomerInterviewService.MAXIMUM_SEARCH_COST_USD)
            > 0
        || modelInvocations < 0
        || modelInvocations > ProductDiscoveryCustomerInterviewService.MAXIMUM_MODEL_INVOCATIONS) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O aprofundamento excedeu ou não comprovou os limites de consultas, busca e chamadas de modelo");
    }
  }

  /** Valida uma tentativa e devolve seu consumo estimado para o teto cumulativo. */
  private GapPlanUsage validateAttemptPlan(ProductDiscoveryCycle cycle, JsonNode plan) {
    JsonNode gaps = plan.path("candidateGaps");
    JsonNode limits = plan.path("researchLimits");
    if (!plan.isObject() || !gaps.isArray() || gaps.isEmpty() || !limits.isObject()) {
      throw new IllegalArgumentException("Estrutura de aprofundamento ausente");
    }
    int plannedQueries = 0;
    BigDecimal plannedCost = BigDecimal.ZERO;
    Set<String> candidates = new HashSet<>();
    Set<String> candidateQueries = new HashSet<>();
    for (JsonNode gap : gaps) {
      requireJsonText(gap, "candidateName");
      requireJsonText(gap, "pendingQuestion");
      requireJsonText(gap, "appropriateSource");
      requireJsonText(gap, "evidenceNeeded");
      requireJsonText(gap, "contraryEvidenceToSeek");
      JsonNode queries = gap.path("publicQueries");
      int maxQueries = gap.path("maxPublicQueries").asInt(0);
      BigDecimal maxCost = decimal(gap.path("maxEstimatedSearchCostUsd"));
      if (!queries.isArray()
          || queries.isEmpty()
          || queries.size() > maxQueries
          || maxQueries < 1
          || maxQueries > 4
          || maxCost == null
          || maxCost.compareTo(
                  ESTIMATED_SEARCH_COST_PER_REQUEST_USD.multiply(
                      BigDecimal.valueOf(queries.size())))
              != 0) {
        throw new IllegalArgumentException("Limite inválido em lacuna candidata");
      }
      if (!candidates.add(normalizeIdentity(gap.path("candidateName").asText()))) {
        throw new IllegalArgumentException("Candidata duplicada no plano de lacunas");
      }
      for (JsonNode query : queries) {
        if (!query.isTextual() || !StringUtils.hasText(query.asText())) {
          throw new IllegalArgumentException("Consulta pública vazia em lacuna candidata");
        }
        candidateQueries.add(normalizeIdentity(query.asText()));
      }
      plannedQueries += queries.size();
      plannedCost = plannedCost.add(maxCost);
    }
    Set<String> expected =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId()).stream()
            .map(item -> normalizeIdentity(item.getName()))
            .collect(Collectors.toSet());
    Set<String> declaredQueries = jsonTextSet(plan.path("publicQueries"));
    BigDecimal attemptCostLimit =
        ProductDiscoveryCustomerInterviewService.MAXIMUM_SEARCH_COST_USD.divide(
            BigDecimal.valueOf(
                ProductDiscoveryCustomerInterviewService.MAXIMUM_DEEPENING_ATTEMPTS));
    if (!candidates.equals(expected)
        || gaps.size() != expected.size()
        || !candidateQueries.equals(declaredQueries)
        || candidateQueries.size() != plannedQueries
        || plannedQueries
            > ProductDiscoveryCustomerInterviewService.MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT
        || plannedCost.compareTo(attemptCostLimit) > 0
        || limits.path("maxPublicQueries").asInt(-1)
            != ProductDiscoveryCustomerInterviewService.MAXIMUM_PUBLIC_QUERIES_PER_ATTEMPT
        || decimal(limits.path("maxEstimatedSearchCostUsd")) == null
        || decimal(limits.path("maxEstimatedSearchCostUsd")).compareTo(attemptCostLimit) != 0) {
      throw new IllegalArgumentException("Plano não cobre candidatas ou excede a política");
    }
    return new GapPlanUsage(candidateQueries, plannedCost);
  }

  /** Recupera do histórico persistido todas as consultas propostas para a execução. */
  private Map<String, Set<String>> planQueriesByCandidate(ProductDiscoveryCycle cycle) {
    try {
      JsonNode root = JSON.readTree(cycle.getResearchPlanJson());
      Map<String, Set<String>> result = new LinkedHashMap<>();
      if (root.path("attempts").isArray()) {
        for (JsonNode attempt : root.path("attempts")) {
          collectPlanQueries(
              attempt.path("plan"),
              result,
              REJECTED_REPEATED_RESEARCH_LENS.equals(attempt.path("planDisposition").asText()));
        }
      } else {
        collectPlanQueries(root, result, false);
      }
      return result;
    } catch (Exception ex) {
      LOGGER.error(
          "[product-discovery] Falha ao reler consultas do plano cycleId={}", cycle.getId(), ex);
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O plano persistido não permite auditar as consultas por candidata",
          ex);
    }
  }

  /** Agrega consultas executadas e propostas rejeitadas mantendo a candidata que as justificou. */
  private void collectPlanQueries(
      JsonNode plan, Map<String, Set<String>> result, boolean repeatedPlanRejected) {
    JsonNode gaps = plan.path("candidateGaps");
    if (!gaps.isArray() || gaps.isEmpty()) {
      throw new IllegalArgumentException("Plano sem lacunas por candidata");
    }
    for (JsonNode gap : gaps) {
      String candidateName = normalizeIdentity(gap.path("candidateName").asText());
      Set<String> queries = jsonTextSet(gap.path("publicQueries"));
      if (!StringUtils.hasText(candidateName) || queries.isEmpty()) {
        throw new IllegalArgumentException("Plano sem candidata ou consultas auditáveis");
      }
      Set<String> cumulative = result.computeIfAbsent(candidateName, ignored -> new HashSet<>());
      if (!repeatedPlanRejected && queries.stream().anyMatch(query -> !cumulative.add(query))) {
        throw new IllegalArgumentException("Plano repetiu consulta para a mesma candidata");
      }
      if (repeatedPlanRejected) cumulative.addAll(queries);
    }
  }

  /** Lê consultas textuais sem aceitar duplicidade silenciosa no relatório final. */
  private Set<String> jsonTextSet(JsonNode values) {
    if (!values.isArray() || values.isEmpty()) return Set.of();
    Set<String> result = new HashSet<>();
    for (JsonNode value : values) {
      if (!value.isTextual()
          || !StringUtils.hasText(value.asText())
          || !result.add(normalizeIdentity(value.asText()))) {
        return Set.of();
      }
    }
    return result;
  }

  /** Lê uma lista auditável que pode ser vazia, mas nunca duplicada ou malformada. */
  private Set<String> jsonTextSetAllowEmpty(JsonNode values) {
    if (!values.isArray()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O relatório de aprofundamento possui lista de evidências inválida");
    }
    Set<String> result = new HashSet<>();
    for (JsonNode value : values) {
      if (!value.isTextual()
          || !StringUtils.hasText(value.asText())
          || !result.add(normalizeIdentity(value.asText()))) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "O relatório de aprofundamento possui evidência vazia ou repetida");
      }
    }
    return result;
  }

  /** Normaliza identidade persistida apenas para comparações de contrato. */
  private String normalizeIdentity(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
  }

  /** Lê decimal JSON sem converter campo ausente em custo zero. */
  private BigDecimal decimal(JsonNode node) {
    return node != null && node.isNumber() ? node.decimalValue() : null;
  }

  /** Exige texto JSON não vazio no plano candidato-específico. */
  private void requireJsonText(JsonNode node, String field) {
    if (!node.path(field).isTextual() || !StringUtils.hasText(node.path(field).asText())) {
      throw new IllegalArgumentException("Campo obrigatório ausente: " + field);
    }
  }

  /** Converte ausência estrutural no relatório final em rejeição contratual. */
  private void requireReportText(JsonNode node, String field) {
    if (!node.path(field).isTextual() || !StringUtils.hasText(node.path(field).asText())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "O relatório de aprofundamento não informou o campo obrigatório " + field);
    }
  }

  /** Resume consultas e custo estimado de uma tentativa validada. */
  private record GapPlanUsage(Set<String> queries, BigDecimal estimatedCost) {}

  /** Distingue plano autorizado de proposta repetida preservada sem nova coleta ou cobrança. */
  private record GapPlanAttempt(JsonNode plan, boolean rejectedRepeatedLens) {}
}

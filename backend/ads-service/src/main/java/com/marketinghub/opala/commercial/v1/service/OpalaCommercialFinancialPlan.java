package com.marketinghub.opala.commercial.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: resolver o plano financeiro vigente da versão comercial exata do Opala. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialFinancialPlan {
  private final FinancialPlanRevisionRepository financialPlans;
  private final CommercialPlanRepository commercialPlans;
  private final CommercialPlanVersionRepository commercialPlanVersions;
  private final ObjectMapper json;

  /**
   * Expõe uma revisão imutável ou uma causa determinística antes de qualquer chamada paga de
   * Plutus.
   */
  public ObjectNode snapshot(OpalaCommercialContext.Scope scope) {
    var result = json.createObjectNode();
    result.put("contractVersion", "OPALA_FINANCIAL_PLAN_V1");
    var product = scope.experiment().getProduct();
    var allowedPlanIds = new HashSet<>(commercialPlans.findIdsByProductId(product.getId()));
    var selected =
        financialPlans
            .findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
                "PRODUCT", product.getId(), Environment.LIVE)
            .stream()
            .filter(plan -> allowedPlanIds.contains(plan.getCommercialPlanId()))
            .filter(
                plan ->
                    Objects.equals(
                        scope.cycle().getProductVersion(), assumptions(plan).productVersion()))
            .findFirst();
    if (selected.isEmpty())
      return unavailable(
          result,
          "MISSING",
          "Cadastre pela tela um plano financeiro LIVE da versão exata e do plano comercial deste produto.");
    var plan = selected.orElseThrow();
    var assumptions = assumptions(plan);
    var evaluation = evaluation(plan);
    result.put("id", plan.getId());
    result.put("revision", plan.getRevisionNumber());
    result.put("name", plan.getName());
    result.put("commercialPlanId", plan.getCommercialPlanId());
    result.put("commercialPlanVersion", plan.getCommercialPlanVersion());
    result.put("createdAt", plan.getCreatedAt().toString());
    result.set("assumptions", json.valueToTree(assumptions));
    result.set("deterministicEvaluation", json.valueToTree(evaluation));
    if (isStale(plan, assumptions))
      return unavailable(
          result, "STALE", "A revisão financeira venceu ou o plano comercial mudou.");
    if (!"PROJECTED_VIABLE".equals(evaluation.status()))
      return unavailable(
          result,
          "NOT_VIABLE",
          "A projeção determinística possui custos ausentes ou cenário inviável.");
    if (scope.experiment().getUnitPrice() == null
        || assumptions.priceBrl() == null
        || scope.experiment().getUnitPrice().compareTo(assumptions.priceBrl()) != 0)
      return unavailable(
          result, "PRICE_MISMATCH", "O preço do plano financeiro diverge do experimento.");
    result.put("status", "READY");
    result.put(
        "reason",
        "Revisão financeira vigente, completa e da mesma versão; ainda requer parecer de Plutus.");
    return result;
  }

  /** Confere validade temporal e versão do plano comercial sem depender do fluxo pago de Plutus. */
  private boolean isStale(FinancialPlanRevision plan, PlanAssumptions assumptions) {
    if (assumptions.validUntil().isBefore(LocalDate.now(ZoneOffset.UTC))) return true;
    return commercialPlanVersions
        .findTopByPlanIdOrderByVersionNumberDesc(plan.getCommercialPlanId())
        .map(
            version -> !Objects.equals(version.getVersionNumber(), plan.getCommercialPlanVersion()))
        .orElse(true);
  }

  /** Lê as premissas imutáveis e preserva contexto completo quando a revisão estiver corrompida. */
  private PlanAssumptions assumptions(FinancialPlanRevision plan) {
    return read(plan, plan.getAssumptionsJson(), PlanAssumptions.class);
  }

  /** Lê o cálculo determinístico persistido sem disparar nova avaliação ou chamada de agente. */
  private PlanEvaluation evaluation(FinancialPlanRevision plan) {
    return read(plan, plan.getEvaluationJson(), PlanEvaluation.class);
  }

  /**
   * Desserializa um contrato financeiro sem introduzir dependência circular no contexto de tarefa.
   */
  private <T> T read(FinancialPlanRevision plan, String value, Class<T> type) {
    try {
      return json.readValue(value, type);
    } catch (Exception ex) {
      log.error(
          "Opala: falha ao ler contrato financeiro financialPlanId={} revision={} type={}",
          plan.getId(),
          plan.getRevisionNumber(),
          type.getSimpleName(),
          ex);
      throw new IllegalStateException("Não foi possível ler o plano financeiro do Opala.", ex);
    }
  }

  /** Registra indisponibilidade sem omitir a revisão encontrada e suas evidências. */
  private ObjectNode unavailable(ObjectNode result, String status, String reason) {
    result.put("status", status);
    result.put("reason", reason);
    return result;
  }
}

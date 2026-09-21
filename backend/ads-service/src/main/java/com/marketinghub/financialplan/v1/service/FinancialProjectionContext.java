package com.marketinghub.financialplan.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation.ScenarioResult;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions.FixedCostCoverage;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: montar e validar a base econômica estruturada enviada a Plutus. */
final class FinancialProjectionContext {
  private static final Logger log = LoggerFactory.getLogger(FinancialProjectionContext.class);
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  /** Impede instanciação do montador puro. */
  private FinancialProjectionContext() {}

  /**
   * Consolida meta, contrato de entrega, envelopes e cenários de sensibilidade sem prever vendas.
   */
  static Result build(
      Product product,
      CommercialPlan plan,
      int commercialPlanVersion,
      PlanView financialPlan,
      ObjectMapper json) {
    var blockers = new ArrayList<String>();
    var assumptions = financialPlan.assumptions();
    var validation = validation(product, json, blockers);
    var price = assumptions.priceBrl();
    var variable = assumptions.variableCostEnvelope();
    var fixed = assumptions.fixedCostEnvelope();
    boolean aggregate = variable != null;

    if (aggregate && fixed == null) {
      blockers.add(
          "Plutus / responsável financeiro: renove o envelope de custos fixos da versão comercial.");
    } else if (aggregate) {
      String expected =
          "commercial-plan:"
              + plan.getId()
              + "@v"
              + commercialPlanVersion
              + ":fixedOperationalCostBrl";
      if (fixed.coverage() != FixedCostCoverage.ALL_FIXED_OPERATIONAL_COSTS_FOR_PERIOD
          || !expected.equals(fixed.sourceReference())
          || plan.getFixedOperationalCostBrl() == null
          || fixed.amountPerPeriodBrl().compareTo(plan.getFixedOperationalCostBrl()) != 0)
        blockers.add(
            "Plutus / responsável financeiro: o envelope fixo diverge do plano comercial vigente.");
    }

    Integer milestoneCustomers =
        positiveInteger(validation.at("/successEvidence/firstMilestoneSales"));
    Integer targetCustomers = exactCustomers(plan.getTargetRevenue(), price);
    Integer operationalCustomers = exactCustomers(plan.getOperationalRevenueTarget(), price);
    ScenarioResult baseScenario = scenario(financialPlan, "BASE");
    if (aggregate) {
      if (plan.getTargetRevenue() != null && targetCustomers == null)
        blockers.add(
            "Atena / responsável pelo produto: a receita-alvo não corresponde a clientes inteiros no preço vigente.");
      if (plan.getOperationalRevenueTarget() != null && operationalCustomers == null)
        blockers.add(
            "Atena / responsável pelo produto: a meta operacional não corresponde a clientes inteiros no preço vigente.");
      if (milestoneCustomers == null && targetCustomers == null)
        blockers.add(
            "Atena / responsável pelo produto: registre uma meta de clientes ou receita para o cenário-base.");
      if (milestoneCustomers != null
          && targetCustomers != null
          && !milestoneCustomers.equals(targetCustomers))
        blockers.add(
            "Atena / responsável pelo produto: a meta de vendas diverge da receita-alvo e do preço.");
      if (operationalCustomers != null
          && targetCustomers != null
          && !operationalCustomers.equals(targetCustomers))
        blockers.add(
            "Atena / responsável pelo produto: as metas financeira e operacional divergem.");
    }
    Integer baseCustomers =
        aggregate
            ? milestoneCustomers != null ? milestoneCustomers : targetCustomers
            : baseScenario == null ? null : baseScenario.customers();

    String formatType = text(validation.at("/format/type"));
    String valueUnit = text(validation.at("/format/valueUnit"));
    String deliveryMode = text(validation.at("/delivery/mode"));
    Boolean personalization = booleanValue(validation.at("/delivery/personalization"));
    boolean fixedPackage = fixedPackage(formatType, valueUnit);
    if (aggregate && !fixedPackage)
      blockers.add(
          "Responsável pelo produto: defina uma unidade fixa de entrega para validar o uso intenso do pacote.");
    if (assumptions.preparation() != null
        && assumptions.preparation().personalizedAi()
        && !Boolean.TRUE.equals(personalization))
      blockers.add(
          "Responsável pelo produto: a personalização financeira diverge do contrato de entrega.");

    BigDecimal contribution =
        aggregate || baseScenario == null ? null : baseScenario.contributionAfterCacBrl();
    BigDecimal fixedAmount =
        aggregate
            ? fixed == null ? null : fixed.amountPerPeriodBrl()
            : assumptions.costs().fixedPerPeriodBrl();
    if (aggregate && price != null && assumptions.maximumCacBrl() != null)
      contribution =
          price.subtract(variable.amountPerCustomerBrl()).subtract(assumptions.maximumCacBrl());
    Integer operatingBreakEven = ceilingCustomers(fixedAmount, contribution);
    BigDecimal baseProfit =
        aggregate
            ? baseCustomers == null || contribution == null || fixedAmount == null
                ? null
                : contribution
                    .multiply(BigDecimal.valueOf(baseCustomers))
                    .subtract(fixedAmount)
                    .setScale(2, RoundingMode.HALF_UP)
            : baseScenario == null ? null : baseScenario.resultAfterInvestmentBrl();
    if (baseProfit == null || baseProfit.signum() <= 0)
      blockers.add("Plutus / Atena: a meta comercial vigente não produz resultado-base positivo.");

    BigDecimal historicalTotal = plan.getActualTotalCost();
    String historicalPeriod =
        aggregate
            ? historicalPeriod(plan, financialPlan, historicalTotal, blockers)
            : "NOT_APPLIED_TO_DECLARED_SCENARIOS";
    Integer recoveryCustomers =
        ceilingCustomers(
            fixedAmount == null || historicalTotal == null
                ? null
                : fixedAmount.add(historicalTotal),
            contribution);
    int conservativeCustomers =
        aggregate
            ? operatingBreakEven == null ? 0 : Math.max(0, operatingBreakEven - 1)
            : customers(financialPlan, "CONSERVATIVE");
    Integer optimisticCustomers =
        aggregate
            ? baseCustomers == null
                ? recoveryCustomers
                : recoveryCustomers == null
                    ? baseCustomers
                    : Math.max(baseCustomers, recoveryCustomers)
            : customers(financialPlan, "OPTIMISTIC");
    BigDecimal incrementalInvestment =
        aggregate
            ? ZERO
            : add(assumptions.costs().initialAiBrl(), assumptions.costs().initialOtherBrl());

    var context = new LinkedHashMap<String, Object>();
    context.put(
        "analysisScope",
        map(
            "code",
            aggregate
                ? "EXISTING_PRODUCT_VERSION_INCREMENTAL_SALE"
                : "DECLARED_FINANCIAL_PLAN_SCENARIOS",
            "productVersion",
            assumptions.productVersion(),
            "incrementalInitialInvestmentBrl",
            incrementalInvestment,
            "incrementalInvestmentSource",
            aggregate
                ? "financial-plan:"
                    + financialPlan.id()
                    + "@revision-"
                    + financialPlan.revision()
                    + ":reuse-existing-version"
                : "financial-plan:"
                    + financialPlan.id()
                    + "@revision-"
                    + financialPlan.revision()
                    + ":declared-initial-costs",
            "condition",
            aggregate
                ? "A análise não autoriza nova produção nem novo investimento; qualquer necessidade nova invalida o parecer."
                : "O investimento declarado na revisão detalhada permanece incluído no resultado dos cenários."));
    context.put(
        "commercialPlanningBasis",
        map(
            "sourceReference",
            "commercial-plan:" + plan.getId() + "@v" + commercialPlanVersion,
            "targetRevenueBrl",
            plan.getTargetRevenue(),
            "operationalRevenueTargetBrl",
            plan.getOperationalRevenueTarget(),
            "firstMilestoneSales",
            milestoneCustomers,
            "baseCustomers",
            baseCustomers,
            "interpretation",
            "CONDITIONAL_COMMERCIAL_TARGET_NOT_DEMAND_FORECAST"));
    context.put(
        "deliveryContract",
        map(
            "sourceReference",
            "product:"
                + product.getId()
                + "@"
                + product.getValidationDefinitionVersion()
                + ":validation-contract",
            "formatType",
            formatType,
            "valueUnit",
            valueUnit,
            "deliveryMode",
            deliveryMode,
            "personalization",
            personalization,
            "costUnit",
            aggregate
                ? fixedPackage ? "FULL_CONTRACTED_PACKAGE_PER_CUSTOMER" : null
                : "DETAILED_DELIVERY_LIMITS",
            "intensiveBoundary",
            aggregate
                ? fixedPackage
                    ? "FULL_CONTRACTED_PACKAGE_ALREADY_COVERED_BY_VARIABLE_ENVELOPE"
                    : null
                : "MAXIMUM_ATTEMPTS_DECLARED_IN_FINANCIAL_PLAN"));
    context.put(
        "costCoverage",
        map(
            "variableEnvelope",
            variable,
            "fixedEnvelope",
            fixed,
            "realizedLedgerRole",
            "RECOVERY_AND_MONITORING_NOT_PROJECTION_ENVELOPE_REPLACEMENT",
            "historicalPeriodClassification",
            historicalPeriod,
            "historicalCampaignCostBrl",
            plan.getActualCampaignCost(),
            "historicalAiCostBrl",
            plan.getActualAiCost(),
            "historicalTotalCostBrl",
            historicalTotal,
            "historicalCostTreatment",
            "VISIBLE_IN_CUMULATIVE_RECOVERY_NOT_RECHARGED_PER_NEW_SALE"));
    context.put(
        "scenarioBasis",
        map(
            "nature",
            aggregate
                ? "DETERMINISTIC_SENSITIVITY_NOT_DEMAND_FORECAST"
                : "DETERMINISTIC_FINANCIAL_PLAN_SCENARIOS",
            "contributionAfterCacPerCustomerBrl",
            contribution,
            "operatingBreakEvenCustomers",
            operatingBreakEven,
            "conservativeCustomers",
            conservativeCustomers,
            "baseCustomers",
            baseCustomers,
            "optimisticRecoveryCustomers",
            optimisticCustomers,
            "baseProfitBrl",
            baseProfit,
            "baseRevenueBrl",
            baseCustomers == null || price == null
                ? null
                : price.multiply(BigDecimal.valueOf(baseCustomers)),
            "rule",
            aggregate
                ? "Conservador testa uma venda abaixo do equilíbrio operacional; BASE usa a meta comercial explícita; otimista mede a recuperação dos custos históricos conhecidos."
                : "Os três volumes, limites de uso, CAC e investimento pertencem à revisão financeira detalhada."));
    context.put(
        "revalidationTriggers",
        List.of(
            "mudança de preço, produto ou versão comercial",
            "novo investimento ou nova produção",
            "mudança de pacote, personalização, quota ou qualidade",
            "mudança de CAC, custo variável ou custo fixo",
            "custo realizado fora dos envelopes ou margem não positiva"));
    return new Result(Map.copyOf(context), List.copyOf(blockers));
  }

  /** Lê o contrato do produto e transforma corrupção ou ausência em bloqueio acionável. */
  private static JsonNode validation(Product product, ObjectMapper json, List<String> blockers) {
    try {
      if (product.getValidationDefinitionJson() == null
          || product.getValidationDefinitionJson().isBlank()) {
        blockers.add("Responsável pelo produto: registre o contrato de validação da versão.");
        return json.createObjectNode();
      }
      JsonNode result = json.readTree(product.getValidationDefinitionJson());
      if (result == null || !result.isObject()) {
        blockers.add("Responsável pelo produto: o contrato de validação precisa ser um objeto.");
        return json.createObjectNode();
      }
      return result;
    } catch (JsonProcessingException ex) {
      log.warn(
          "Plano financeiro: contrato de validação inválido productId={}", product.getId(), ex);
      blockers.add("Responsável pelo produto: corrija o contrato de validação inválido.");
      return json.createObjectNode();
    }
  }

  /** Deriva clientes somente quando receita e preço formam uma quantidade inteira exata. */
  private static Integer exactCustomers(BigDecimal targetRevenue, BigDecimal price) {
    if (targetRevenue == null || price == null || price.signum() <= 0) return null;
    try {
      return targetRevenue.divide(price, 0, RoundingMode.UNNECESSARY).intValueExact();
    } catch (ArithmeticException ex) {
      log.debug(
          "Meta de receita não forma quantidade inteira. targetRevenue={} price={}",
          targetRevenue,
          price,
          ex);
      return null;
    }
  }

  /** Calcula o primeiro número inteiro de clientes que cobre o valor informado. */
  private static Integer ceilingCustomers(BigDecimal amount, BigDecimal contribution) {
    if (amount == null || contribution == null || contribution.signum() <= 0) return null;
    return amount.divide(contribution, 0, RoundingMode.CEILING).intValueExact();
  }

  /** Classifica custos realizados como históricos somente após o encerramento do plano original. */
  private static String historicalPeriod(
      CommercialPlan plan,
      PlanView financialPlan,
      BigDecimal historicalTotal,
      List<String> blockers) {
    if (historicalTotal == null || historicalTotal.signum() == 0)
      return "NO_RECORDED_HISTORICAL_COST";
    var revisionDate = financialPlan.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
    if (plan.getDeadline() != null && plan.getDeadline().isBefore(revisionDate))
      return "CLOSED_BEFORE_CURRENT_FINANCIAL_REVISION";
    blockers.add(
        "Plutus / responsável financeiro: classifique custos realizados que podem pertencer ao novo período.");
    return "CURRENT_OR_OVERLAPPING_PERIOD_REQUIRES_CLASSIFICATION";
  }

  /** Reconhece apenas unidade contratual fechada, sem inferir franquia de uso contínuo. */
  private static boolean fixedPackage(String formatType, String valueUnit) {
    String type = formatType == null ? "" : formatType.toUpperCase();
    String unit = valueUnit == null ? "" : valueUnit.toLowerCase();
    return type.contains("PACK")
        || type.contains("KIT")
        || unit.contains("kit")
        || unit.contains("pacote");
  }

  /** Lê inteiro estritamente positivo do contrato. */
  private static Integer positiveInteger(JsonNode node) {
    return node != null && node.canConvertToInt() && node.asInt() > 0 ? node.asInt() : null;
  }

  /** Lê texto não vazio sem converter ausência em literal. */
  private static String text(JsonNode node) {
    return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
  }

  /** Lê booleano apenas quando o contrato declara o tipo correto. */
  private static Boolean booleanValue(JsonNode node) {
    return node != null && node.isBoolean() ? node.asBoolean() : null;
  }

  /** Localiza um cenário calculado pelo código canônico, sem depender da ordem da lista. */
  private static ScenarioResult scenario(PlanView financialPlan, String code) {
    if (financialPlan.evaluation() == null || financialPlan.evaluation().scenarios() == null)
      return null;
    return financialPlan.evaluation().scenarios().stream()
        .filter(candidate -> code.equals(candidate.code()))
        .findFirst()
        .orElse(null);
  }

  /** Obtém o volume calculado de um cenário ou zero quando a revisão não o declarou. */
  private static int customers(PlanView financialPlan, String code) {
    ScenarioResult result = scenario(financialPlan, code);
    return result == null || result.customers() == null ? 0 : result.customers();
  }

  /** Soma custos declarados preservando a ausência de qualquer parcela. */
  private static BigDecimal add(BigDecimal first, BigDecimal second) {
    return first == null || second == null ? null : first.add(second);
  }

  /** Monta mapa ordenado que aceita campos nulos para preservar lacunas auditáveis. */
  private static Map<String, Object> map(Object... values) {
    var result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
    return result;
  }

  /** Resultado do preflight com contexto seguro e impedimentos que evitam chamada paga. */
  record Result(Map<String, Object> context, List<String> blockers) {
    /** Informa se todas as fontes mínimas foram comprovadas deterministicamente. */
    boolean ready() {
      return blockers.isEmpty();
    }
  }
}

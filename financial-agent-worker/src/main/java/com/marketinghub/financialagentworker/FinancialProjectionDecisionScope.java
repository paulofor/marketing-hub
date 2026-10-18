package com.marketinghub.financialagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Responsabilidade: transportar ao prompt o escopo de projeção declarado pelo backend. */
final class FinancialProjectionDecisionScope {
  /** Impede instanciar o resolvedor do contrato de entrada. */
  private FinancialProjectionDecisionScope() {}

  /** Confere o aceite estruturado antes do modelo, sem produzir ou alterar um parecer. */
  static String resolve(String context, ObjectMapper json) throws IOException {
    if (context == null || !context.stripLeading().startsWith("{")) return "ECONOMIC_VIABILITY";
    JsonNode input = json.readTree(context);
    if (!"DISCOVERY_REVIEW".equals(input.at("/deterministicEvaluation/status").asText()))
      return "ECONOMIC_VIABILITY";
    JsonNode discovery = input.at("/projectionBasis/discoveryInvestment");
    if (!positiveId(input.path("financialPlanId"))
        || !positiveId(input.path("productId"))
        || !discovery.isObject()
        || !positiveId(discovery.path("experimentId"))
        || !positiveId(discovery.path("learningCycleId"))
        || !discovery.path("acceptBelowMinimumMargin").isBoolean()
        || !discovery.path("acceptBelowMinimumMargin").booleanValue()
        || !discovery.path("mediaBudgetBrl").isNumber()
        || discovery.path("mediaBudgetBrl").decimalValue().signum() <= 0
        || !positiveId(discovery.path("durationDays"))
        || discovery.path("durationDays").longValue() > 30
        || !discovery.path("decisionReference").isTextual()
        || discovery.path("decisionReference").asText().isBlank())
      throw new IllegalArgumentException(
          "DISCOVERY_REVIEW exige aceite estruturado válido do backend antes do modelo.");
    return "LIMITED_DISCOVERY";
  }

  /** Aceita somente identificadores e durações inteiros positivos do contrato. */
  private static boolean positiveId(JsonNode value) {
    return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0;
  }
}

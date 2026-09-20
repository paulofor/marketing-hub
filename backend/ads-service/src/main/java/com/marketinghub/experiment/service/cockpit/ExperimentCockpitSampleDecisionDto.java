package com.marketinghub.experiment.service.cockpit;

import java.math.BigDecimal;

/** Expõe a decisão progressiva de amostra que separa evidência comercial e proteção financeira. */
public record ExperimentCockpitSampleDecisionDto(
    boolean applicable,
    boolean measurementAvailable,
    String measurementSource,
    String status,
    String headline,
    String explanation,
    long humanVisitors,
    int initialTargetVisitors,
    long visitorsRemainingForInitialDecision,
    int targetPurchasesAtInitialDecision,
    int precisionTargetVisitors,
    long visitorsRemainingForPrecisionDecision,
    long purchases,
    BigDecimal observedPurchaseRatePercent,
    BigDecimal confidenceLower95Percent,
    BigDecimal confidenceUpper95Percent,
    BigDecimal zeroPurchaseUpper95Percent,
    BigDecimal estimatedCostPerHumanVisitor,
    BigDecimal projectedSpendForInitialTarget,
    BigDecimal projectedSpendForPrecisionTarget,
    BigDecimal zeroPrimaryResultStopSpend,
    BigDecimal mediaSpendLimit,
    Boolean initialTargetFitsMediaSpendLimit,
    Boolean precisionTargetFitsMediaSpendLimit,
    String projectionConfidence,
    String financialGuardrail,
    String recommendation) {
  /** Aplica o limite autorizado sem alterar a leitura estatística ou as metas comerciais. */
  public ExperimentCockpitSampleDecisionDto withZeroResultStopSpend(BigDecimal value) {
    return new ExperimentCockpitSampleDecisionDto(
        applicable,
        measurementAvailable,
        measurementSource,
        status,
        headline,
        explanation,
        humanVisitors,
        initialTargetVisitors,
        visitorsRemainingForInitialDecision,
        targetPurchasesAtInitialDecision,
        precisionTargetVisitors,
        visitorsRemainingForPrecisionDecision,
        purchases,
        observedPurchaseRatePercent,
        confidenceLower95Percent,
        confidenceUpper95Percent,
        zeroPurchaseUpper95Percent,
        estimatedCostPerHumanVisitor,
        projectedSpendForInitialTarget,
        projectedSpendForPrecisionTarget,
        value,
        mediaSpendLimit,
        initialTargetFitsMediaSpendLimit,
        precisionTargetFitsMediaSpendLimit,
        projectionConfidence,
        financialGuardrail,
        recommendation);
  }
}

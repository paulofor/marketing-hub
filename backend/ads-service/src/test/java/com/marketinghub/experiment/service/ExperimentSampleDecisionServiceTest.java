package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Testa a decisão progressiva de amostra comercial sem executar ações financeiras. */
class ExperimentSampleDecisionServiceTest {

  private final ExperimentSampleDecisionService service =
      new ExperimentSampleDecisionService(new ExperimentBinomialConfidenceService());

  /** Reproduz Vega com quatro visitantes e impede conclusão prematura sobre oferta ou página. */
  @Test
  void keepsVegaInInsufficientDataWithFourHumanVisitors() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 4, 0),
            0,
            new BigDecimal("5.89"));

    assertThat(decision.status()).isEqualTo("INSUFFICIENT_DATA");
    assertThat(decision.humanVisitors()).isEqualTo(4);
    assertThat(decision.initialTargetVisitors()).isEqualTo(100);
    assertThat(decision.visitorsRemainingForInitialDecision()).isEqualTo(96);
    assertThat(decision.targetPurchasesAtInitialDecision()).isEqualTo(5);
    assertThat(decision.precisionTargetVisitors()).isEqualTo(500);
    assertThat(decision.visitorsRemainingForPrecisionDecision()).isEqualTo(496);
    assertThat(decision.zeroPurchaseUpper95Percent()).isEqualByComparingTo("52.71");
    assertThat(decision.estimatedCostPerHumanVisitor()).isEqualByComparingTo("1.4725");
    assertThat(decision.projectedSpendForInitialTarget()).isEqualByComparingTo("147.25");
    assertThat(decision.projectedSpendForPrecisionTarget()).isEqualByComparingTo("736.25");
    assertThat(decision.zeroPrimaryResultStopSpend()).isEqualByComparingTo("25.00");
    assertThat(decision.initialTargetFitsMediaSpendLimit()).isFalse();
    assertThat(decision.projectionConfidence()).isEqualTo("PRELIMINARY");
  }

  /** Usa compras da mesma coorte PDE mesmo quando o placar agregado possui outro valor. */
  @Test
  void prefersAttributedPurchasesFromTheMeasuredCohort() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 4, 0),
            4,
            new BigDecimal("5.89"));

    assertThat(decision.purchases()).isZero();
    assertThat(decision.status()).isEqualTo("INSUFFICIENT_DATA");
  }

  /** Confirma a evidência contra meta de 5% quando cem visitantes terminam sem compra. */
  @Test
  void rejectsInitialVersionWhenZeroPurchaseUpperBoundFallsBelowTarget() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 100),
            0,
            new BigDecimal("100.00"));

    assertThat(decision.status()).isEqualTo("INITIAL_ZERO_SALES_REJECTED");
    assertThat(decision.zeroPurchaseUpper95Percent()).isEqualByComparingTo("2.95");
    assertThat(decision.observedPurchaseRatePercent()).isEqualByComparingTo("0.00");
  }

  /** Trata cinco compras em cem visitantes como sinal inicial, nunca como escala automática. */
  @Test
  void marksFiveSalesInOneHundredVisitorsAsInitialSignal() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 100),
            5,
            new BigDecimal("90.00"));

    assertThat(decision.status()).isEqualTo("INITIAL_TARGET_REACHED");
    assertThat(decision.observedPurchaseRatePercent()).isEqualByComparingTo("5.00");
    assertThat(decision.confidenceLower95Percent()).isEqualByComparingTo("1.64");
    assertThat(decision.confidenceUpper95Percent()).isEqualByComparingTo("11.28");
    assertThat(decision.recommendation()).contains("nova autorização financeira");
  }

  /** Libera somente a revisão de escala após vinte e cinco compras em quinhentos visitantes. */
  @Test
  void marksPrecisionRoundReadyAtFiveHundredVisitors() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 500),
            25,
            new BigDecimal("700.00"));

    assertThat(decision.status()).isEqualTo("PRECISION_TARGET_REACHED");
    assertThat(decision.confidenceLower95Percent()).isEqualByComparingTo("3.26");
    assertThat(decision.confidenceUpper95Percent()).isEqualByComparingTo("7.29");
    assertThat(decision.recommendation()).contains("solicitar autorização");
  }

  /** Bloqueia interpretação quando compras atribuídas excedem visitantes humanos. */
  @Test
  void blocksInconsistentPurchasesAndVisitors() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 4),
            5,
            new BigDecimal("5.89"));

    assertThat(decision.status()).isEqualTo("MEASUREMENT_INVALID");
    assertThat(decision.measurementAvailable()).isTrue();
  }

  /** Mantém explícita a incerteza quando a primeira amostra termina com poucas compras. */
  @Test
  void keepsIntermediateInitialResultBelowTargetWithoutScaling() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 100, 3),
            0,
            new BigDecimal("100.00"));

    assertThat(decision.status()).isEqualTo("INITIAL_TARGET_NOT_REACHED");
    assertThat(decision.observedPurchaseRatePercent()).isEqualByComparingTo("3.00");
    assertThat(decision.recommendation()).startsWith("Não escalar");
  }

  /** Bloqueia conclusão quando a coorte humana atribuída não está disponível. */
  @Test
  void blocksDecisionWhenMeasurementIsUnavailable() {
    var decision =
        service.evaluate(
            salesExperiment(),
            ExperimentSampleDecisionService.SampleMeasurement.unavailable(
                "PDE_ATTRIBUTED_HUMAN_COHORT"),
            0,
            new BigDecimal("5.89"));

    assertThat(decision.status()).isEqualTo("MEASUREMENT_UNAVAILABLE");
    assertThat(decision.measurementAvailable()).isFalse();
  }

  /** Bloqueia conclusão quando a meta estatística do experimento não foi configurada. */
  @Test
  void blocksDecisionWhenSampleConfigurationIsMissing() {
    Experiment experiment = salesExperiment();
    experiment.setSampleSize(null);

    var decision =
        service.evaluate(
            experiment,
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "PDE_ATTRIBUTED_HUMAN_COHORT", 4, 0),
            0,
            new BigDecimal("5.89"));

    assertThat(decision.status()).isEqualTo("CONFIGURATION_REQUIRED");
  }

  /** Mantém abordagem individual fora da estratégia de aquisição paga. */
  @Test
  void keepsDirectChannelOnItsOwnSampleContract() {
    Experiment directExperiment = salesExperiment();
    directExperiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);

    var decision =
        service.evaluate(
            directExperiment,
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "LANDING_ATTRIBUTED_HUMAN_VISITORS", 10),
            1,
            BigDecimal.ZERO);

    assertThat(decision.applicable()).isFalse();
    assertThat(decision.status()).isEqualTo("NOT_APPLICABLE");
  }

  /** Preserva outros funis até existir uma coorte com visitantes e vendas líquidas conciliadas. */
  @Test
  void keepsNonPdeSalesFunnelOutsideTheStrategy() {
    Experiment landingExperiment = salesExperiment();
    landingExperiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);

    var decision =
        service.evaluate(
            landingExperiment,
            ExperimentSampleDecisionService.SampleMeasurement.available(
                "LANDING_ATTRIBUTED_HUMAN_VISITORS", 100),
            5,
            new BigDecimal("90.00"));

    assertThat(decision.applicable()).isFalse();
    assertThat(decision.status()).isEqualTo("NOT_APPLICABLE");
  }

  /** Cria o contrato mínimo de um experimento de vendas comparável ao Vega. */
  private Experiment salesExperiment() {
    return Experiment.builder()
        .id(92L)
        .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
        .campaignObjective(ExperimentCampaignObjective.SALES)
        .platform(ExperimentPlatform.FACEBOOK)
        .sampleSize(100)
        .targetCvr(new BigDecimal("5.00"))
        .mediaSpendLimit(new BigDecimal("100.00"))
        .build();
  }
}

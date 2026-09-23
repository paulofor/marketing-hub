package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateEvaluatorType;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateSeverity;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.product.Product;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialChecks;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: provar o mapeamento entre cada atividade técnica e sua fonte persistida. */
class ExperimentTechnicalPreflightEvidenceServiceTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final ExperimentRunGateResultRepository gates =
      mock(ExperimentRunGateResultRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final QuartzoPreflightEvidenceScopeService quartzoEvidence =
      mock(QuartzoPreflightEvidenceScopeService.class);
  private final QuartzoCommercialContext quartzoContext = mock(QuartzoCommercialContext.class);
  private final QuartzoCommercialChecks quartzoChecks = mock(QuartzoCommercialChecks.class);
  private final ObjectMapper json = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC);
  private final Product product = Product.builder().id(7L).build();
  private final Experiment experiment = new Experiment();
  private final ExperimentRun run = new ExperimentRun();
  private ExperimentTechnicalPreflightEvidenceService service;

  /** Prepara um run produtivo e todos os gates aprovados para Capella sintética. */
  @BeforeEach
  void setup() {
    experiment.setId(88L);
    experiment.setProduct(product);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setUnitPrice(new BigDecimal("67.00"));
    experiment.setDailyBudget(new BigDecimal("20.00"));
    experiment.setMediaSpendLimit(new BigDecimal("125.00"));
    experiment.setZeroResultSpendLimit(new BigDecimal("50.00"));
    experiment.setZeroPurchaseSpendLimit(new BigDecimal("50.00"));
    experiment.setPurchaseStopCount(5);
    experiment.setStartDate(LocalDate.of(2026, 9, 23));
    experiment.setEndDate(LocalDate.of(2026, 9, 29));
    run.setId(12L);
    run.setExperiment(experiment);
    run.setRunNumber(2);
    run.setMode(ExperimentRunMode.PRODUCTION);
    run.setStatus(ExperimentRunStatus.READY_TO_PUBLISH);
    run.setDataQualityStatus(ExperimentRunDataQualityStatus.VALID);
    run.setEvidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED);
    run.setPreflightCompletedAt(Instant.parse("2026-09-22T20:00:00Z"));
    when(experiments.findById(88L)).thenReturn(Optional.of(experiment));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(88L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(12L)).thenReturn(allGates());
    var plan = new CommercialPlan();
    plan.setId(2L);
    plan.setStatus(CommercialPlanStatus.IN_PROGRESS);
    plan.setMaxBudget(new BigDecimal("400.00"));
    when(plans.findByExperimentReference(88L)).thenReturn(List.of(plan));
    service =
        new ExperimentTechnicalPreflightEvidenceService(
            experiments,
            runs,
            gates,
            plans,
            quartzoEvidence,
            quartzoContext,
            quartzoChecks,
            json,
            clock);
  }

  /** Vincula superfície, transação e medição somente aos gates correspondentes. */
  @Test
  void mapsEachTechnicalActivityToItsApprovedEvidence() {
    var surfaces = service.evaluate("surfaces", product, "experiment:88");
    var transaction = service.evaluate("transaction", product, "experiment:88");
    var measurement = service.evaluate("measurement", product, "experiment:88");

    assertThat(surfaces.objectiveEvidence().path("gates").size()).isEqualTo(1);
    assertThat(surfaces.objectiveEvidence().path("gates").get(0).path("code").asText())
        .isEqualTo(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED);
    assertThat(transaction.objectiveEvidence().path("gates").get(0).path("code").asText())
        .isEqualTo(ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED);
    assertThat(measurement.objectiveEvidence().path("gates")).hasSize(2);
    assertThat(measurement.inputFingerprint()).hasSize(64);
    assertThat(measurement.objectiveEvidence().path("evaluatedAt").asText())
        .isEqualTo("2026-09-23T12:00:00Z");
  }

  /** Recusa gate sem evidência mesmo quando seu status textual foi aprovado. */
  @Test
  void blocksApprovedGateWithoutVerifiableReference() {
    var invalid = gate(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED);
    invalid.setEvidenceReference(" ");
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(12L))
        .thenReturn(List.of(invalid));

    assertThatThrownBy(() -> service.evaluate("surfaces", product, "experiment:88"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("evidência verificável");
  }

  /** Recusa pixels de outra publicação Quartzo antes de materializar a superfície. */
  @Test
  void blocksStaleQuartzoPublication() {
    when(quartzoEvidence.applies(run)).thenReturn(true);
    when(quartzoEvidence.hasCurrentEvidence(run)).thenReturn(false);

    assertThatThrownBy(() -> service.evaluate("surfaces", product, "experiment:88"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("mudaram após a homologação");
  }

  /** Revalida Plutus e os limites persistidos sem conceder nova autorização de gasto. */
  @Test
  void validatesFinancialPlanAndQuartzoEconomics() {
    when(quartzoContext.applies(product)).thenReturn(true);
    var scope = new QuartzoCommercialContext.Scope(experiment, product, "v1", null, 19L, null);
    var snapshot = json.createObjectNode().put("fingerprint", "commercial-sha");
    when(quartzoContext.scope("experiment:88", 7L, false)).thenReturn(scope);
    when(quartzoContext.snapshot("experiment:88")).thenReturn(snapshot);

    var evidence = service.evaluate("financialGuardrails", product, "experiment:88");

    verify(quartzoChecks).check("economics", scope, snapshot);
    assertThat(evidence.objectiveEvidence().path("mediaSpendLimitBrl").decimalValue())
        .isEqualByComparingTo("125.00");
    assertThat(evidence.objectiveEvidence().path("spendAuthorizedByThisActivity").asBoolean())
        .isFalse();
    assertThat(evidence.objectiveEvidence().path("commercialFingerprint").asText())
        .isEqualTo("commercial-sha");
  }

  /** Impede que o teto operacional ultrapasse o plano governante. */
  @Test
  void blocksFinancialLimitAboveCommercialPlan() {
    var plan = new CommercialPlan();
    plan.setStatus(CommercialPlanStatus.IN_PROGRESS);
    plan.setMaxBudget(new BigDecimal("100.00"));
    when(plans.findByExperimentReference(88L)).thenReturn(List.of(plan));

    assertThatThrownBy(() -> service.evaluate("financialGuardrails", product, "experiment:88"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ultrapassa o plano");
  }

  /** Impede que rascunho ou plano bloqueado seja tratado como autorização governante. */
  @Test
  void blocksFinancialGuardrailsWithoutActiveGoverningPlan() {
    var draft = new CommercialPlan();
    draft.setStatus(CommercialPlanStatus.DRAFT);
    draft.setMaxBudget(new BigDecimal("400.00"));
    var blocked = new CommercialPlan();
    blocked.setStatus(CommercialPlanStatus.BLOCKED);
    blocked.setMaxBudget(new BigDecimal("400.00"));
    when(plans.findByExperimentReference(88L)).thenReturn(List.of(draft, blocked));

    assertThatThrownBy(() -> service.evaluate("financialGuardrails", product, "experiment:88"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("em execução ou concluído");
  }

  /** Impede que a mesma referência técnica seja reutilizada por outro produto. */
  @Test
  void blocksExperimentFromAnotherProduct() {
    assertThatThrownBy(
            () -> service.evaluate("surfaces", Product.builder().id(10L).build(), "experiment:88"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("outro produto");
  }

  /** Monta os quatro gates funcionais necessários à homologação de venda na Meta. */
  private List<ExperimentRunGateResult> allGates() {
    return List.of(
        gate(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED),
        gate(ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED),
        gate(ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED),
        gate(ExperimentRunGateCodes.DATA_FRESHNESS_VALID));
  }

  /** Cria um gate aprovado com identidade completa da evidência. */
  private ExperimentRunGateResult gate(String code) {
    return ExperimentRunGateResult.builder()
        .id((long) code.hashCode())
        .experimentRun(run)
        .gateCode(code)
        .gateGroup(ExperimentRunGateGroup.FUNCTIONAL_E2E)
        .status(ExperimentRunGateStatus.PASS)
        .severity(ExperimentRunGateSeverity.INFO)
        .summary("Prova funcional de " + code)
        .evidenceReference("evidence:" + code)
        .evaluatedAt(Instant.parse("2026-09-22T19:00:00Z"))
        .evaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC)
        .evaluatorVersion("experiment-run-homologation.v1")
        .build();
  }
}

package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions.*;
import com.marketinghub.planning.CommercialPlanVersion;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: provar a seleção financeira por produto, plano e versão do Opala. */
class OpalaCommercialFinancialPlanTest {
  private final FinancialPlanRevisionRepository financialPlans =
      mock(FinancialPlanRevisionRepository.class);
  private final CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
  private final CommercialPlanVersionRepository commercialPlanVersions =
      mock(CommercialPlanVersionRepository.class);
  private final OpalaCommercialFinancialPlan resolver =
      new OpalaCommercialFinancialPlan(
          financialPlans,
          commercialPlans,
          commercialPlanVersions,
          new ObjectMapper().findAndRegisterModules());
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Experiment experiment =
      Experiment.builder()
          .id(92L)
          .unitPrice(new BigDecimal("67"))
          .product(Product.builder().id(4L).build())
          .build();

  /** Prepara a identidade financeira sem exceções baseadas no identificador do caso original. */
  @BeforeEach
  void setup() {
    cycle.setProductVersion("fixture-v12");
    when(commercialPlans.findIdsByProductId(4L)).thenReturn(List.of(3L));
    var version = new CommercialPlanVersion();
    version.setVersionNumber(7);
    when(commercialPlanVersions.findTopByPlanIdOrderByVersionNumberDesc(3L))
        .thenReturn(Optional.of(version));
  }

  /** Seleciona a revisão viável da mesma versão e expõe os cálculos ao agente. */
  @Test
  void selectsReadyPlanForExactVersion() {
    when(financialPlans.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", 4L, Environment.LIVE))
        .thenReturn(List.of(plan("fixture-v12", false, "PROJECTED_VIABLE")));

    var result = resolver.snapshot(new OpalaCommercialContext.Scope(cycle, experiment));

    assertThat(result.path("status").asText()).isEqualTo("READY");
    assertThat(result.path("id").asLong()).isEqualTo(81L);
    assertThat(result.path("assumptions").path("priceBrl").decimalValue())
        .isEqualByComparingTo("67");
    assertThat(result.path("deterministicEvaluation").path("scenarios")).hasSize(1);
  }

  /** Outra versão não é usada como fallback e evita repetir a falha da tarefa 436. */
  @Test
  void refusesPlanFromPredecessorVersion() {
    when(financialPlans.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", 4L, Environment.LIVE))
        .thenReturn(List.of(plan("fixture-v11", false, "PROJECTED_VIABLE")));

    var result = resolver.snapshot(new OpalaCommercialContext.Scope(cycle, experiment));

    assertThat(result.path("status").asText()).isEqualTo("MISSING");
  }

  /** Revisão vencida ou cálculo inviável continua visível, mas não libera Plutus. */
  @Test
  void refusesStaleAndUnviablePlans() {
    var oldVersion = new CommercialPlanVersion();
    oldVersion.setVersionNumber(8);
    when(commercialPlanVersions.findTopByPlanIdOrderByVersionNumberDesc(3L))
        .thenReturn(Optional.of(oldVersion));
    when(financialPlans.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", 4L, Environment.LIVE))
        .thenReturn(List.of(plan("fixture-v12", false, "PROJECTED_VIABLE")));
    assertThat(
            resolver
                .snapshot(new OpalaCommercialContext.Scope(cycle, experiment))
                .path("status")
                .asText())
        .isEqualTo("STALE");

    var currentVersion = new CommercialPlanVersion();
    currentVersion.setVersionNumber(7);
    when(commercialPlanVersions.findTopByPlanIdOrderByVersionNumberDesc(3L))
        .thenReturn(Optional.of(currentVersion));
    when(financialPlans.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", 4L, Environment.LIVE))
        .thenReturn(List.of(plan("fixture-v12", false, "REVIEW_REQUIRED")));
    assertThat(
            resolver
                .snapshot(new OpalaCommercialContext.Scope(cycle, experiment))
                .path("status")
                .asText())
        .isEqualTo("NOT_VIABLE");
  }

  /** Monta uma revisão financeira completa para a validação do resolvedor. */
  private FinancialPlanRevision plan(String productVersion, boolean stale, String status) {
    var assumptions =
        new PlanAssumptions(
            productVersion,
            90,
            LocalDate.of(2099, 10, 31),
            "Fontes financeiras completas.",
            new AiCost(
                Currency.BRL,
                "MUSA_LOCAL_RULES_V1",
                BigDecimal.ZERO,
                "Telemetria local",
                LocalDate.of(2026, 9, 17),
                null,
                null,
                7,
                0,
                BigDecimal.ZERO),
            new Costs(
                new BigDecimal("6.99"),
                new BigDecimal("33"),
                BigDecimal.ZERO,
                new BigDecimal("12"),
                new BigDecimal("2.49"),
                new BigDecimal("5"),
                new BigDecimal("0.10"),
                new BigDecimal("0.40"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO),
            new BigDecimal("67"),
            new BigDecimal("10"),
            new BigDecimal("15"),
            List.of(
                new Scenario(ScenarioCode.CONSERVATIVE, 3, 0, new BigDecimal("15")),
                new Scenario(ScenarioCode.BASE, 5, 0, new BigDecimal("15")),
                new Scenario(ScenarioCode.OPTIMISTIC, 8, 0, new BigDecimal("10"))));
    var scenario =
        new PlanEvaluation.ScenarioResult(
            "BASE",
            "Base",
            5,
            0,
            new BigDecimal("335"),
            new BigDecimal("148.3835"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("5.5"),
            new BigDecimal("24.1767"),
            new BigDecimal("9.1767"),
            new BigDecimal("30.92"),
            new BigDecimal("21.2090"),
            new BigDecimal("45.8835"),
            new BigDecimal("45.8835"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            true,
            List.of());
    if (stale)
      assumptions =
          new PlanAssumptions(
              assumptions.productVersion(),
              assumptions.periodDays(),
              LocalDate.of(2020, 1, 1),
              assumptions.evidence(),
              assumptions.ai(),
              assumptions.costs(),
              assumptions.priceBrl(),
              assumptions.minimumMarginPercent(),
              assumptions.maximumCacBrl(),
              assumptions.scenarios());
    var mapper = new ObjectMapper().findAndRegisterModules();
    var plan = new FinancialPlanRevision();
    plan.setId(81L);
    plan.setScopeKind("PRODUCT");
    plan.setScopeId(4L);
    plan.setEnvironment(Environment.LIVE);
    plan.setName("Vega v12");
    plan.setRevisionNumber(1);
    plan.setCommercialPlanId(3L);
    plan.setCommercialPlanVersion(7);
    plan.setCreatedAt(Instant.parse("2026-09-17T12:00:00Z"));
    try {
      plan.setAssumptionsJson(mapper.writeValueAsString(assumptions));
      plan.setEvaluationJson(
          mapper.writeValueAsString(
              new PlanEvaluation(status, "Viável", List.of(), List.of(scenario))));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return plan;
  }
}

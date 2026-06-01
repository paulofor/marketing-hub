package com.marketinghub.epm;

import com.marketinghub.repository.jpa.epm.ExperimentBudgetRepository;
import com.marketinghub.repository.jpa.epm.ExperimentFinancialDecisionRepository;
import com.marketinghub.repository.jpa.epm.ExperimentFinancialMetricRepository;
import com.marketinghub.repository.jpa.epm.FinancialPlanHypothesisRepository;
import com.marketinghub.repository.jpa.epm.FinancialPlanNicheRepository;
import com.marketinghub.repository.jpa.epm.FinancialPlanRepository;
import com.marketinghub.repository.jpa.epm.ProductPriceScenarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida a persistência básica das entidades e repositórios da Sprint 1 do EPM.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class EpmRepositoryTest {

    @Autowired
    FinancialPlanRepository financialPlanRepository;

    @Autowired
    FinancialPlanNicheRepository financialPlanNicheRepository;

    @Autowired
    FinancialPlanHypothesisRepository financialPlanHypothesisRepository;

    @Autowired
    ExperimentBudgetRepository experimentBudgetRepository;

    @Autowired
    ExperimentFinancialMetricRepository experimentFinancialMetricRepository;

    @Autowired
    ExperimentFinancialDecisionRepository experimentFinancialDecisionRepository;

    @Autowired
    ProductPriceScenarioRepository productPriceScenarioRepository;

    /** Verifica se a hierarquia financeira do EPM persiste valores monetários em centavos. */
    @Test
    void shouldPersistFinancialHierarchyWithBudgetInCents() {
        FinancialPlan plan = financialPlanRepository.save(FinancialPlan.builder()
                .name("Plano Junho 2026")
                .cycleStartDate(LocalDate.of(2026, 6, 1))
                .cycleEndDate(LocalDate.of(2026, 6, 30))
                .totalBudgetCents(300_000L)
                .defaultDailyBudgetCents(2_000L)
                .defaultExperimentDurationDays(3)
                .defaultExperimentsPerHypothesis(3)
                .status(FinancialPlanStatus.ACTIVE)
                .build());

        FinancialPlanNiche niche = financialPlanNicheRepository.save(FinancialPlanNiche.builder()
                .financialPlan(plan)
                .externalNicheId(15L)
                .nicheName("Personal trainers")
                .plannedBudgetCents(60_000L)
                .spendLimitCents(60_000L)
                .status(FinancialPlanNicheStatus.TESTING)
                .build());

        FinancialPlanHypothesis hypothesis = financialPlanHypothesisRepository.save(FinancialPlanHypothesis.builder()
                .financialPlanNiche(niche)
                .externalHypothesisId("8f35c480-6f4d-4bc3-8f09-2bd911f4a928")
                .title("Agenda cheia sem desconto")
                .plannedExperiments(3)
                .plannedCostPerExperimentCents(6_000L)
                .lossLimitCents(18_000L)
                .status(FinancialPlanHypothesisStatus.TESTING)
                .build());

        ExperimentBudget budget = experimentBudgetRepository.save(ExperimentBudget.builder()
                .financialPlanHypothesis(hypothesis)
                .externalExperimentId(42L)
                .name("Experimento 1")
                .plannedDailyBudgetCents(2_000L)
                .plannedDurationDays(3)
                .plannedTotalBudgetCents(6_000L)
                .status(ExperimentBudgetStatus.RUNNING)
                .build());

        ExperimentBudget saved = experimentBudgetRepository.findById(budget.getId()).orElseThrow();

        assertThat(saved.getPlannedTotalBudgetCents()).isEqualTo(6_000L);
        assertThat(saved.getFinancialPlanHypothesis().getLossLimitCents()).isEqualTo(18_000L);
        assertThat(saved.getFinancialPlanHypothesis().getFinancialPlanNiche().getPlannedBudgetCents()).isEqualTo(60_000L);
        assertThat(saved.getFinancialPlanHypothesis().getFinancialPlanNiche().getFinancialPlan().getTotalBudgetCents())
                .isEqualTo(300_000L);
    }

    /** Verifica se métricas, decisões e cenários de preço persistem com os vínculos corretos. */
    @Test
    void shouldPersistFinancialMetricsDecisionAndPriceScenario() {
        FinancialPlan plan = financialPlanRepository.save(FinancialPlan.builder()
                .name("Plano Validação")
                .cycleStartDate(LocalDate.of(2026, 6, 1))
                .cycleEndDate(LocalDate.of(2026, 6, 15))
                .totalBudgetCents(120_000L)
                .status(FinancialPlanStatus.ACTIVE)
                .build());
        FinancialPlanNiche niche = financialPlanNicheRepository.save(FinancialPlanNiche.builder()
                .financialPlan(plan)
                .nicheName("Nutricionistas")
                .plannedBudgetCents(30_000L)
                .spendLimitCents(30_000L)
                .build());
        FinancialPlanHypothesis hypothesis = financialPlanHypothesisRepository.save(FinancialPlanHypothesis.builder()
                .financialPlanNiche(niche)
                .title("Cardápio rápido para pacientes")
                .plannedExperiments(2)
                .plannedCostPerExperimentCents(5_000L)
                .lossLimitCents(10_000L)
                .build());
        ExperimentBudget budget = experimentBudgetRepository.save(ExperimentBudget.builder()
                .financialPlanHypothesis(hypothesis)
                .name("Criativo benefício direto")
                .plannedDailyBudgetCents(2_500L)
                .plannedDurationDays(2)
                .plannedTotalBudgetCents(5_000L)
                .build());

        ExperimentFinancialMetric metric = experimentFinancialMetricRepository.save(ExperimentFinancialMetric.builder()
                .experimentBudget(budget)
                .measuredAt(Instant.parse("2026-06-01T12:00:00Z"))
                .visitors(100)
                .leads(12)
                .checkoutClicks(3)
                .purchases(1)
                .adSpendCents(5_000L)
                .revenueCents(9_700L)
                .paymentFeeCents(800L)
                .platformFeeCents(0L)
                .aiCostCents(200L)
                .taxEstimateCents(600L)
                .grossProfitCents(4_700L)
                .estimatedNetProfitCents(4_100L)
                .build());
        ExperimentFinancialDecision decision = experimentFinancialDecisionRepository.save(ExperimentFinancialDecision.builder()
                .experimentBudget(budget)
                .decisionType(ExperimentFinancialDecisionType.SCALE_CONTROLLED)
                .reason("Experimento gerou compra real com lucro bruto positivo.")
                .decidedAt(Instant.parse("2026-06-01T13:00:00Z"))
                .decidedBy("codex-test")
                .build());
        ProductPriceScenario scenario = productPriceScenarioRepository.save(ProductPriceScenario.builder()
                .financialPlan(plan)
                .name("Produto R$97")
                .priceCents(9_700L)
                .expectedPaymentFeeCents(800L)
                .expectedPlatformFeeCents(0L)
                .expectedTaxCents(600L)
                .expectedNetRevenuePerSaleCents(8_300L)
                .totalBudgetCents(120_000L)
                .breakEvenSales(15)
                .build());

        assertThat(experimentFinancialMetricRepository.findById(metric.getId()))
                .get()
                .extracting(ExperimentFinancialMetric::getEstimatedNetProfitCents)
                .isEqualTo(4_100L);
        assertThat(experimentFinancialDecisionRepository.findById(decision.getId()))
                .get()
                .extracting(ExperimentFinancialDecision::getDecisionType)
                .isEqualTo(ExperimentFinancialDecisionType.SCALE_CONTROLLED);
        assertThat(productPriceScenarioRepository.findById(scenario.getId()))
                .get()
                .extracting(ProductPriceScenario::getBreakEvenSales)
                .isEqualTo(15);
    }
}

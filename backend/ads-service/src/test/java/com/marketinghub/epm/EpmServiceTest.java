package com.marketinghub.epm;

import com.marketinghub.epm.service.EpmService;
import com.marketinghub.epm.service.createExperimentBudget.CreateExperimentBudgetRequest;
import com.marketinghub.epm.service.createExperimentMetric.CreateExperimentMetricRequest;
import com.marketinghub.epm.service.createProductPriceScenario.CreateProductPriceScenarioRequest;
import com.marketinghub.repository.jpa.epm.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Valida os cálculos operacionais da Sprint 2 do serviço EPM.
 */
class EpmServiceTest {

    /** Verifica se orçamento total e restante são calculados ao criar um experimento. */
    @Test
    void shouldCalculatePlannedTotalAndRemainingBudget() {
        FinancialPlanHypothesisRepository hypothesisRepository = mock(FinancialPlanHypothesisRepository.class);
        ExperimentBudgetRepository budgetRepository = mock(ExperimentBudgetRepository.class);
        EpmService service = service(null, null, hypothesisRepository, budgetRepository, null, null, null);
        FinancialPlanHypothesis hypothesis = FinancialPlanHypothesis.builder().id(3L).build();
        when(hypothesisRepository.findById(3L)).thenReturn(Optional.of(hypothesis));
        when(budgetRepository.save(any())).thenAnswer(invocation -> {
            ExperimentBudget budget = invocation.getArgument(0);
            budget.setId(9L);
            return budget;
        });

        var response = service.createExperimentBudget(3L, new CreateExperimentBudgetRequest(42L, "Experimento", 2_000L, 3, null, LocalDate.of(2026, 6, 1), null, ExperimentBudgetStatus.RUNNING, null));

        assertThat(response.plannedTotalBudgetCents()).isEqualTo(6_000L);
        assertThat(response.remainingBudgetCents()).isEqualTo(6_000L);
        assertThat(response.status()).isEqualTo(ExperimentBudgetStatus.RUNNING);
    }

    /** Verifica se lucro, ROAS e métricas por custo são calculados sem dividir por zero. */
    @Test
    void shouldCalculateManualMetricProfitAndRatios() {
        ExperimentBudgetRepository budgetRepository = mock(ExperimentBudgetRepository.class);
        ExperimentFinancialMetricRepository metricRepository = mock(ExperimentFinancialMetricRepository.class);
        EpmService service = service(null, null, null, budgetRepository, metricRepository, null, null);
        ExperimentBudget budget = ExperimentBudget.builder().id(5L).spendLimitCents(6_000L).actualSpendCents(0L).remainingBudgetCents(6_000L).build();
        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(metricRepository.save(any())).thenAnswer(invocation -> {
            ExperimentFinancialMetric metric = invocation.getArgument(0);
            metric.setId(7L);
            metric.setCreatedAt(Instant.parse("2026-06-01T12:00:00Z"));
            return metric;
        });

        var response = service.createExperimentMetric(5L, new CreateExperimentMetricRequest(Instant.parse("2026-06-01T10:00:00Z"), 1_000L, 50L, 100, 10, 2, 3, 1, 6_000L, 9_700L, 800L, 0L, 200L, 600L, null));

        assertThat(response.grossProfitCents()).isEqualTo(3_700L);
        assertThat(response.estimatedNetProfitCents()).isEqualTo(2_100L);
        assertThat(response.cplCents()).isEqualTo(600L);
        assertThat(response.cpaCents()).isEqualTo(6_000L);
        assertThat(response.roasDecimal()).isEqualByComparingTo("1.6167");
        assertThat(response.landingConversionDecimal()).isEqualByComparingTo("0.100000");
        assertThat(budget.getRemainingBudgetCents()).isZero();
    }

    /** Verifica se cenário de preço calcula receita líquida por venda e ponto de equilíbrio. */
    @Test
    void shouldCalculateBreakEvenSalesForPriceScenario() {
        FinancialPlanRepository planRepository = mock(FinancialPlanRepository.class);
        ProductPriceScenarioRepository scenarioRepository = mock(ProductPriceScenarioRepository.class);
        EpmService service = service(planRepository, null, null, null, null, null, scenarioRepository);
        FinancialPlan plan = FinancialPlan.builder().id(2L).totalBudgetCents(120_000L).build();
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(scenarioRepository.save(any())).thenAnswer(invocation -> {
            ProductPriceScenario scenario = invocation.getArgument(0);
            scenario.setId(4L);
            return scenario;
        });

        var response = service.createProductPriceScenario(2L, new CreateProductPriceScenarioRequest("R$97", 9_700L, 800L, 0L, 600L, null, null));

        assertThat(response.expectedNetRevenuePerSaleCents()).isEqualTo(8_300L);
        assertThat(response.breakEvenSales()).isEqualTo(15);
    }

    /** Monta o serviço com mocks para isolar as regras de cálculo. */
    private EpmService service(FinancialPlanRepository planRepository, FinancialPlanNicheRepository nicheRepository, FinancialPlanHypothesisRepository hypothesisRepository, ExperimentBudgetRepository budgetRepository, ExperimentFinancialMetricRepository metricRepository, ExperimentFinancialDecisionRepository decisionRepository, ProductPriceScenarioRepository scenarioRepository) {
        return new EpmService(
                planRepository == null ? mock(FinancialPlanRepository.class) : planRepository,
                nicheRepository == null ? mock(FinancialPlanNicheRepository.class) : nicheRepository,
                hypothesisRepository == null ? mock(FinancialPlanHypothesisRepository.class) : hypothesisRepository,
                budgetRepository == null ? mock(ExperimentBudgetRepository.class) : budgetRepository,
                metricRepository == null ? mock(ExperimentFinancialMetricRepository.class) : metricRepository,
                decisionRepository == null ? mock(ExperimentFinancialDecisionRepository.class) : decisionRepository,
                scenarioRepository == null ? mock(ProductPriceScenarioRepository.class) : scenarioRepository);
    }
}

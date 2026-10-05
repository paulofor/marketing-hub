package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService;
import com.marketinghub.growthoperator.service.GrowthOperatorService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Responsabilidade: validar a consolidação somente leitura do contexto do Estrategista. */
class ExperimentStrategistContextServiceTest {

  /**
   * Preserva autorização, oferta e versão em planos independentes sem transformar texto em gasto.
   */
  @Test
  void preservesPreparationContextAcrossPlans() {
    var plans = mock(CommercialPlanService.class);
    var versions = mock(CommercialPlanVersionService.class);
    var service =
        new ExperimentStrategistContextService(
            plans,
            mock(GrowthOperatorService.class),
            mock(JdbcTemplate.class),
            mock(ExperimentStrategistMemoryService.class),
            mock(ProductService.class),
            versions);
    for (long id : new long[] {501L, 807L}) {
      var plan = new CommercialPlan();
      plan.setId(id);
      plan.setMainOffer("Uma ocasião, três resultados; acesso por sete dias.");
      plan.setNextAction("Fonte humana: USD 8 somente para preparação.");
      plan.setMaxBudget(java.math.BigDecimal.ZERO);
      when(plans.getPlan(id)).thenReturn(plan);
      when(versions.current(id))
          .thenReturn(
              new CommercialPlanVersionDto(
                  id + 10L, id, 3, "{}", "QA", "Contexto sintético", java.time.Instant.now()));
      var context = (Map<?, ?>) service.researchContext(id).get("commercialPlan");
      assertThat(context.get("mainOffer")).isEqualTo(plan.getMainOffer());
      assertThat(context.get("nextAction")).isEqualTo(plan.getNextAction());
      assertThat(context.get("version")).isEqualTo(3);
      assertThat(context.get("maxBudget")).isEqualTo(java.math.BigDecimal.ZERO);
    }
  }

  /** Garante que sessões, funil, aprendizados e limites sejam entregues no mesmo contrato. */
  @Test
  void shouldConsolidateReadOnlyResearchContext() {
    CommercialPlanService plans = mock(CommercialPlanService.class);
    GrowthOperatorService growth = mock(GrowthOperatorService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ExperimentStrategistMemoryService memory = mock(ExperimentStrategistMemoryService.class);
    ProductService products = mock(ProductService.class);
    when(memory.activeForPlan(1L)).thenReturn(java.util.List.of());
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(1L);
    plan.setName("Agenda Cheia");
    plan.setCurrentBlocker("INSTRUMENTACAO");
    plan.setExperiment(experiment);
    when(plans.getPlan(1L)).thenReturn(plan);
    Product musa = new Product();
    musa.setId(7L);
    musa.setName("MUSA");
    musa.setProductFormat("GUIDED_PROGRAM");
    when(products.listProducts()).thenReturn(List.of(musa));
    when(products.getExperimentComparison(7L))
        .thenReturn(
            new ProductExperimentComparisonResponse(
                7L,
                "MUSA",
                "musa",
                "VALIDATION",
                "GUIDED_PROGRAM",
                "HYBRID",
                "ONE_TIME",
                "7 dias",
                "Conclusão",
                "v1",
                "Ainda sem vendas.",
                List.of()));
    when(growth.sessionIntelligence(1L, 2000)).thenReturn(Map.of("available", true));
    when(growth.videoStrategyIntelligence(1L)).thenReturn(Map.of("strategies", List.of()));
    when(jdbc.queryForList(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(81L)))
        .thenReturn(List.of(Map.of("summary", "Visitante analisou e saiu.")));

    Map<String, Object> result =
        new ExperimentStrategistContextService(plans, growth, jdbc, memory, products)
            .researchContext(1L);

    assertThat(result.get("authorityMode")).isEqualTo("READ_ONLY_RESEARCH");
    assertThat(result.get("sessionsAndFunnel")).isEqualTo(Map.of("available", true));
    assertThat((List<?>) result.get("learnings")).hasSize(1);
    assertThat((List<?>) result.get("productPortfolio")).hasSize(1);
    List<String> prohibitedActions =
        ((List<?>) result.get("prohibitedActions")).stream().map(String::valueOf).toList();
    assertThat(prohibitedActions)
        .contains("PRICE", "CAMPAIGN", "BUDGET", "PUBLICATION", "MASS_COMMUNICATION");
    verify(growth).sessionIntelligence(1L, 2000);
  }
}

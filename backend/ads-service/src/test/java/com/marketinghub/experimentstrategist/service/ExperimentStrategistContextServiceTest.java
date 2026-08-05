package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.growthoperator.service.GrowthOperatorService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Responsabilidade: validar a consolidação somente leitura do contexto do Estrategista. */
class ExperimentStrategistContextServiceTest {

  /** Garante que sessões, funil, aprendizados e limites sejam entregues no mesmo contrato. */
  @Test
  void shouldConsolidateReadOnlyResearchContext() {
    CommercialPlanService plans = mock(CommercialPlanService.class);
    GrowthOperatorService growth = mock(GrowthOperatorService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(1L);
    plan.setName("Agenda Cheia");
    plan.setCurrentBlocker("INSTRUMENTACAO");
    plan.setExperiment(experiment);
    when(plans.getPlan(1L)).thenReturn(plan);
    when(growth.sessionIntelligence(1L, 2000)).thenReturn(Map.of("available", true));
    when(growth.videoStrategyIntelligence(1L)).thenReturn(Map.of("strategies", List.of()));
    when(jdbc.queryForList(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(81L)))
        .thenReturn(List.of(Map.of("summary", "Visitante analisou e saiu.")));

    Map<String, Object> result =
        new ExperimentStrategistContextService(plans, growth, jdbc).researchContext(1L);

    assertThat(result.get("authorityMode")).isEqualTo("READ_ONLY_RESEARCH");
    assertThat(result.get("sessionsAndFunnel")).isEqualTo(Map.of("available", true));
    assertThat((List<?>) result.get("learnings")).hasSize(1);
    List<String> prohibitedActions =
        ((List<?>) result.get("prohibitedActions")).stream().map(String::valueOf).toList();
    assertThat(prohibitedActions)
        .contains("PRICE", "CAMPAIGN", "BUDGET", "PUBLICATION", "MASS_COMMUNICATION");
    verify(growth).sessionIntelligence(1L, 2000);
  }
}

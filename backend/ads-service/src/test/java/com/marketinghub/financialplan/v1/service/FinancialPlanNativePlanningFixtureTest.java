package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.InternalAgentTaskExecutionController;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Responsabilidade: detectar dependências ausentes no controller real da fixture financeira sem
 * banco ou API paga.
 */
class FinancialPlanNativePlanningFixtureTest {
  /** Inicializa os beans efetivos e preserva o planejamento sintético sem autorização de vídeo. */
  @Test
  void initializesNativeControllerWithExplicitSpendingDouble() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(ProductRepository.class, () -> mock(ProductRepository.class));
      context.registerBean(
          CommercialPlanRepository.class, () -> mock(CommercialPlanRepository.class));
      context.registerBean(FinancialPlanService.class, () -> mock(FinancialPlanService.class));
      context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
      context.register(FinancialPlanNativePlanningFixture.class);
      context.refresh();
      var controller = context.getBean(InternalAgentTaskExecutionController.class);
      assertThat(controller.spendingContext("experiment-strategist", 95121L).required()).isFalse();
    }
  }
}

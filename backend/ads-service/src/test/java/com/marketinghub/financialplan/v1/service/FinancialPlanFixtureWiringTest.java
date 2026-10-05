package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.openai.service.OpenAiPricingService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: detectar dependências de Plutus ausentes antes da matriz MySQL da fixture. */
class FinancialPlanFixtureWiringTest {
  /** Inicializa o serviço simulado com todas as dependências exigidas pelo Spring. */
  @Test
  void wiresPricingWithoutProductionCatalogOrPaidCalls() {
    var fixture = new FinancialPlanLocalApplication();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(OpenAiPricingService.class, fixture::pricing);
      context.registerBean(
          FinancialAgentService.class, () -> fixture.plutus(mock(DataSource.class)));
      context.refresh();
      assertThat(
              ReflectionTestUtils.getField(context.getBean(FinancialAgentService.class), "pricing"))
          .isSameAs(context.getBean(OpenAiPricingService.class));
    }
  }

  /** Reproduz a configuração antiga e impede ocultar uma dependência obrigatória. */
  @Test
  void rejectsOldFixtureWithoutPricing() {
    var fixture = new FinancialPlanLocalApplication();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(
          FinancialAgentService.class, () -> fixture.plutus(mock(DataSource.class)));
      assertThatThrownBy(context::refresh).hasMessageContaining("pricing");
    }
  }
}

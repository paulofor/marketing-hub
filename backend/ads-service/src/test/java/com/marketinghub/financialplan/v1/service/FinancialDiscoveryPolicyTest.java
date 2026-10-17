package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir transferência de aceites e extrapolação do investimento de descoberta.
 */
class FinancialDiscoveryPolicyTest {
  /** Rejeita produto, ciclo, plano, status, teto e duração incompatíveis em dois contextos. */
  @Test
  void validaIdentidadeELimitesSemAutorizarSucessores() throws Exception {
    for (long owner : new long[] {95121, 95122}) {
      var cycles = mock(LearningSalesCycleRepository.class);
      var experiments = mock(ExperimentRepository.class);
      var plans = mock(CommercialPlanRepository.class);
      var c = new LearningSalesCycle();
      c.setId(owner + 1000);
      c.setProductId(owner);
      c.setExperimentId(owner + 2000);
      c.setStatus("OPEN");
      c.setBudgetLimitBrl(new BigDecimal("100"));
      var product = new Product();
      product.setId(owner);
      var e = new Experiment();
      e.setId(owner + 2000);
      e.setProduct(product);
      e.setStatus(ExperimentStatus.PLANNED);
      e.setMediaSpendLimit(new BigDecimal("100"));
      e.setStartDate(LocalDate.of(2026, 10, 20));
      e.setEndDate(LocalDate.of(2026, 10, 24));
      var plan = new CommercialPlan();
      plan.setId(owner);
      when(cycles.findById(owner + 1000)).thenReturn(Optional.of(c));
      when(experiments.findById(owner + 2000)).thenReturn(Optional.of(e));
      when(plans.findByExperimentReference(e.getId())).thenReturn(List.of(plan));
      var policy = new FinancialDiscoveryPolicy(cycles, experiments, plans);
      var a =
          new ObjectMapper()
              .findAndRegisterModules()
              .treeToValue(
                  FinancialPlanCalculatorTest.discoveryInput(owner), PlanAssumptions.class);
      assertThatCode(() -> policy.validate("PRODUCT", owner, owner, a)).doesNotThrowAnyException();
      assertThatThrownBy(() -> policy.validate("TYPE", owner, owner, a))
          .hasMessageContaining("Modelos");
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner + 1, owner, a))
          .hasMessageContaining("próprio produto");
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner, owner + 1, a))
          .hasMessageContaining("mesmo plano");
      c.setExperimentId(owner + 2001);
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner, owner, a))
          .hasMessageContaining("próprio produto");
      c.setExperimentId(owner + 2000);
      c.setStatus("CLOSED");
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner, owner, a))
          .hasMessageContaining("ciclo aberto");
      c.setStatus("OPEN");
      e.setMediaSpendLimit(new BigDecimal("101"));
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner, owner, a))
          .hasMessageContaining("teto do experimento");
      e.setMediaSpendLimit(new BigDecimal("100"));
      e.setEndDate(LocalDate.of(2026, 10, 25));
      assertThatThrownBy(() -> policy.validate("PRODUCT", owner, owner, a))
          .hasMessageContaining("janela");
    }
  }

  /** Recusa identidades e duração fracionárias sem truncar para outra autorização. */
  @Test
  void contratoRecusaCoercaoDeIdentidadeEDuracao() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    for (String field : List.of("experimentId", "learningCycleId", "durationDays")) {
      for (String invalid : List.of("5.5", "\"5\"")) {
        var node = FinancialPlanCalculatorTest.discoveryInput(95121);
        node.withObject("/discoveryInvestment").set(field, json.readTree(invalid));
        assertThatThrownBy(() -> json.treeToValue(node, PlanAssumptions.class))
            .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
      }
    }
  }
}

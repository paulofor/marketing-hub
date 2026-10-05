package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

/** Responsabilidade: impedir que a persistência transforme consumo pequeno e positivo em zero. */
@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentExecutionCostPersistenceTest {
  @Autowired private EntityManager em;

  /** Faz ida e volta real de custos fracionários e legados nos dois especialistas. */
  @Test
  void preservesPositiveMicroCostsAndLegacyValues() {
    var plan = new CommercialPlan();
    plan.setName("QA de precisão");
    em.persist(plan);
    for (BigDecimal cost :
        new BigDecimal[] {
          new BigDecimal("0.00000250"), new BigDecimal("0.00092800"), new BigDecimal("0.17")
        }) {
      var atena = new ExperimentStrategistExecution();
      atena.setCommercialPlan(plan);
      atena.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
      atena.setAuthorityMode("READ_ONLY_RESEARCH");
      atena.setResearchQuestion("QA isolado");
      atena.setEvidenceSnapshot("{}");
      atena.setEstimatedCost(cost);
      atena.setRawModelResponse("{\"preserved\":true}");
      var plutus = new FinancialAgentExecution();
      plutus.setCommercialPlan(plan);
      plutus.setCommercialPlanVersion(1);
      plutus.setStatus(FinancialAgentExecutionStatus.COMPLETED);
      plutus.setAuthorityMode("READ_ONLY_FINANCIAL_RECONCILIATION");
      plutus.setFinancialSnapshot("{}");
      plutus.setEstimatedCost(cost);
      plutus.setRawModelResponse("{\"preserved\":true}");
      em.persist(atena);
      em.persist(plutus);
      em.flush();
      var atenaId = atena.getId();
      var plutusId = plutus.getId();
      em.clear();
      assertThat(em.find(ExperimentStrategistExecution.class, atenaId).getEstimatedCost())
          .isEqualByComparingTo(cost);
      assertThat(em.find(FinancialAgentExecution.class, plutusId).getEstimatedCost())
          .isEqualByComparingTo(cost);
      assertThat(em.find(ExperimentStrategistExecution.class, atenaId).getRawModelResponse())
          .contains("preserved");
      plan = em.find(CommercialPlan.class, plan.getId());
    }
  }
}

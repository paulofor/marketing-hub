package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ExperimentFinancialDecision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA para persistir e consultar registros ExperimentFinancialDecision do EPM. */
public interface ExperimentFinancialDecisionRepository
    extends JpaRepository<ExperimentFinancialDecision, Long> {
  /** Lista as decisões financeiras vinculadas ao orçamento de experimento informado. */
  List<ExperimentFinancialDecision> findByExperimentBudgetIdOrderByDecidedAtDesc(
      Long experimentBudgetId);
}

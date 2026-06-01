package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ExperimentBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para persistir e consultar registros ExperimentBudget do EPM.
 */
public interface ExperimentBudgetRepository extends JpaRepository<ExperimentBudget, Long> {
    /** Lista os orçamentos de experimento vinculados à hipótese informada. */
    List<ExperimentBudget> findByFinancialPlanHypothesisIdOrderByIdAsc(Long financialPlanHypothesisId);
}

package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ExperimentBudget;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para persistir e consultar registros ExperimentBudget do EPM.
 */
public interface ExperimentBudgetRepository extends JpaRepository<ExperimentBudget, Long> {
}

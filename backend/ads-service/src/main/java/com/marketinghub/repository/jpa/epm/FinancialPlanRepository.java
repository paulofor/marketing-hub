package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.FinancialPlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para persistir e consultar registros FinancialPlan do EPM.
 */
public interface FinancialPlanRepository extends JpaRepository<FinancialPlan, Long> {
}

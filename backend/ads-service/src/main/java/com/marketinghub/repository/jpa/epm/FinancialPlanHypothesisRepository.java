package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.FinancialPlanHypothesis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para persistir e consultar registros FinancialPlanHypothesis do EPM.
 */
public interface FinancialPlanHypothesisRepository extends JpaRepository<FinancialPlanHypothesis, Long> {
    /** Lista as hipóteses financeiras vinculadas ao nicho informado. */
    List<FinancialPlanHypothesis> findByFinancialPlanNicheIdOrderByIdAsc(Long financialPlanNicheId);
}

package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ExperimentFinancialMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para persistir e consultar registros ExperimentFinancialMetric do EPM.
 */
public interface ExperimentFinancialMetricRepository extends JpaRepository<ExperimentFinancialMetric, Long> {
    /** Lista as métricas financeiras vinculadas ao orçamento de experimento informado. */
    List<ExperimentFinancialMetric> findByExperimentBudgetIdOrderByMeasuredAtDesc(Long experimentBudgetId);

    /** Busca a métrica financeira manual mais recente do orçamento de experimento informado. */
    Optional<ExperimentFinancialMetric> findFirstByExperimentBudgetIdOrderByMeasuredAtDesc(Long experimentBudgetId);
}

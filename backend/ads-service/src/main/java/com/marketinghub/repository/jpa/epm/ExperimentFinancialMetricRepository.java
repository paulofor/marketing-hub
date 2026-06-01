package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ExperimentFinancialMetric;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para persistir e consultar registros ExperimentFinancialMetric do EPM.
 */
public interface ExperimentFinancialMetricRepository extends JpaRepository<ExperimentFinancialMetric, Long> {
}

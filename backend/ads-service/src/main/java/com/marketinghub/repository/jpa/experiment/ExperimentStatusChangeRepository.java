package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persiste o histórico auditável de mudanças de status dos experimentos.
 */
public interface ExperimentStatusChangeRepository extends JpaRepository<ExperimentStatusChange, Long> {
}

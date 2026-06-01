package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.targeting.TargetingCandidateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de ExperimentTargetingSelection.
 */
public interface ExperimentTargetingSelectionRepository extends JpaRepository<ExperimentTargetingSelection, Long> {
    List<ExperimentTargetingSelection> findByExperimentIdOrderByCandidateTypeAscTermAsc(Long experimentId);

    long countByExperimentId(Long experimentId);
    long countByExperimentIdAndCandidateType(Long experimentId, TargetingCandidateType candidateType);

    void deleteByExperimentId(Long experimentId);
}

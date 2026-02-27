package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.ExperimentTargetingSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentTargetingSelectionRepository extends JpaRepository<ExperimentTargetingSelection, Long> {
    List<ExperimentTargetingSelection> findByExperimentIdOrderByCandidateTypeAscTermAsc(Long experimentId);

    long countByExperimentId(Long experimentId);

    void deleteByExperimentId(Long experimentId);
}

package com.marketinghub.facebookads.playbook.repository;

import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExperimentAdSetWorkflowRepository extends JpaRepository<ExperimentAdSetWorkflow, Long> {
    Optional<ExperimentAdSetWorkflow> findByExperimentId(Long experimentId);

    List<ExperimentAdSetWorkflow> findByExperimentIdIn(Collection<Long> experimentIds);
}

package com.marketinghub.repository.jpa.facebookads.playbook;

import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de ExperimentAdSetWorkflow. */
public interface ExperimentAdSetWorkflowRepository
    extends JpaRepository<ExperimentAdSetWorkflow, Long> {
  Optional<ExperimentAdSetWorkflow> findByExperimentId(Long experimentId);

  List<ExperimentAdSetWorkflow> findByExperimentIdIn(Collection<Long> experimentIds);
}

package com.marketinghub.facebookads.playbook.repository;

import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExperimentAdSetSpecRepository extends JpaRepository<ExperimentAdSetSpec, Long> {
    List<ExperimentAdSetSpec> findByWorkflowId(Long workflowId);

    Optional<ExperimentAdSetSpec> findByWorkflowIdAndSlot(Long workflowId, ExperimentAdSetSpecSlot slot);

    void deleteByWorkflowId(Long workflowId);
}

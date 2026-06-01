package com.marketinghub.repository.jpa.facebookads.playbook;

import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de ExperimentAdSetSpec.
 */
public interface ExperimentAdSetSpecRepository extends JpaRepository<ExperimentAdSetSpec, Long> {
    List<ExperimentAdSetSpec> findByWorkflowId(Long workflowId);

    Optional<ExperimentAdSetSpec> findByWorkflowIdAndSlot(Long workflowId, ExperimentAdSetSpecSlot slot);

    void deleteByWorkflowId(Long workflowId);
}

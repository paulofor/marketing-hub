package com.marketinghub.repository.jpa.facebookads.playbook;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de ExperimentAdSetJobApiLog.
 */
public interface ExperimentAdSetJobApiLogRepository extends JpaRepository<ExperimentAdSetJobApiLog, Long> {
    void deleteByJobId(Long jobId);

    List<ExperimentAdSetJobApiLog> findByJobIdOrderByCreatedAtAsc(Long jobId);

    List<ExperimentAdSetJobApiLog> findByJobWorkflowExperimentId(Long experimentId, Pageable pageable);
}

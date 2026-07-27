package com.marketinghub.repository.jpa.facebookads.playbook;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de ExperimentAdSetJobApiLog. */
public interface ExperimentAdSetJobApiLogRepository
    extends JpaRepository<ExperimentAdSetJobApiLog, Long> {
  void deleteByJobId(Long jobId);

  List<ExperimentAdSetJobApiLog> findByJobIdOrderByCreatedAtAsc(Long jobId);

  List<ExperimentAdSetJobApiLog> findByJobWorkflowExperimentId(
      Long experimentId, Pageable pageable);
}

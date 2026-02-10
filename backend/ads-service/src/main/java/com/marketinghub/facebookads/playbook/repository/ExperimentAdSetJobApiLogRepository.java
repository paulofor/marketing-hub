package com.marketinghub.facebookads.playbook.repository;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentAdSetJobApiLogRepository extends JpaRepository<ExperimentAdSetJobApiLog, Long> {
    void deleteByJobId(Long jobId);

    List<ExperimentAdSetJobApiLog> findByJobIdOrderByCreatedAtAsc(Long jobId);
}

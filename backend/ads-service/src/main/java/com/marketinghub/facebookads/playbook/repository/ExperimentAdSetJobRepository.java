package com.marketinghub.facebookads.playbook.repository;

import com.marketinghub.facebookads.playbook.ExperimentAdSetJob;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobStatus;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExperimentAdSetJobRepository extends JpaRepository<ExperimentAdSetJob, Long> {

    List<ExperimentAdSetJob> findTop50ByWorkerAndStatusOrderByCreatedAtAsc(ExperimentAdSetWorker worker,
                                                                           ExperimentAdSetJobStatus status);

    Optional<ExperimentAdSetJob> findFirstByWorkflowIdAndTypeOrderByFinishedAtDesc(Long workflowId,
                                                                                  ExperimentAdSetJobType type);

    List<ExperimentAdSetJob> findByWorkflowId(Long workflowId);

    List<ExperimentAdSetJob> findByWorkflowIdAndType(Long workflowId, ExperimentAdSetJobType type);

    boolean existsByWorkflowIdAndTypeAndStatusIn(Long workflowId,
                                                 ExperimentAdSetJobType type,
                                                 Collection<ExperimentAdSetJobStatus> statuses);

    boolean existsByWorkflowIdAndTypeAndResourceIdAndStatusIn(Long workflowId,
                                                          ExperimentAdSetJobType type,
                                                          Long resourceId,
                                                          Collection<ExperimentAdSetJobStatus> statuses);

    void deleteByWorkflowId(Long workflowId);

    @Modifying
    @Query("update ExperimentAdSetJob j set j.status = :running, j.lockedBy = :workerId, j.lockedAt = :now, " +
           "j.startedAt = :now, j.errorMessage = null, j.attemptCount = coalesce(j.attemptCount, 0) + 1 " +
           "where j.id = :jobId and j.status = :expected")
    int claimJob(@Param("jobId") Long jobId,
                 @Param("workerId") String workerId,
                 @Param("now") Instant now,
                 @Param("expected") ExperimentAdSetJobStatus expected,
                 @Param("running") ExperimentAdSetJobStatus running);

    @Modifying
    @Query("update ExperimentAdSetJob j set j.status = :pending, j.lockedBy = null, j.lockedAt = null, j.startedAt = null " +
           "where j.status = :running and j.lockedAt is not null and j.lockedAt < :threshold")
    int releaseExpiredLocks(@Param("threshold") Instant threshold,
                            @Param("running") ExperimentAdSetJobStatus running,
                            @Param("pending") ExperimentAdSetJobStatus pending);
}

package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

public interface TargetingResolutionJobRepository {
    List<TargetingResolutionJobRecord> claimPendingJobs(String workerId, int batchSize);

    void markCompleted(long jobId, int resolvedOptionsCount);

    void markFailed(long jobId, String errorMessage);

    int releaseExpiredLocks(Duration lockTtl);
}

package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmJob;
import com.marketinghub.oprm.OprmJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OprmJobRepository extends JpaRepository<OprmJob, UUID> {
    List<OprmJob> findTop500ByOrderByCreatedAtDesc();

    @Query("""
            select j.id
            from OprmJob j
            where j.jobStatus = com.marketinghub.oprm.OprmJobStatus.PENDING
            order by j.createdAt asc
            """)
    Optional<UUID> findNextPendingJobId();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OprmJob j
            set j.jobStatus = :claimed,
                j.claimedBy = :workerId,
                j.claimedAt = :now,
                j.leaseExpiresAt = :leaseExpiresAt,
                j.attemptCount = j.attemptCount + 1
            where j.id = :jobId
              and j.jobStatus = :pending
            """)
    int claimPendingJob(@Param("jobId") UUID jobId,
                        @Param("workerId") String workerId,
                        @Param("now") Instant now,
                        @Param("leaseExpiresAt") Instant leaseExpiresAt,
                        @Param("pending") OprmJobStatus pending,
                        @Param("claimed") OprmJobStatus claimed);
}

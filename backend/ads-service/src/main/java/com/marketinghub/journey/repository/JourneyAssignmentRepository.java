package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for assignments tying actors to journeys.
 */
public interface JourneyAssignmentRepository extends JpaRepository<JourneyAssignment, Long> {
    Page<JourneyAssignment> findByJourneyId(Long journeyId, Pageable pageable);

    Page<JourneyAssignment> findByJourneyIdAndStatus(Long journeyId, JourneyAssignmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"journey", "journey.template", "nextStep", "lead"})
    @Query("""
            select a from JourneyAssignment a
            join a.journey j
            where a.status in :statuses
              and a.nextStep is not null
              and (a.nextAttemptAt is null or a.nextAttemptAt <= :now)
              and j.status = com.marketinghub.journey.model.JourneyStatus.ACTIVE
              and (j.startAt is null or j.startAt <= :now)
              and (j.endAt is null or j.endAt >= :now)
            order by a.updatedAt asc
            """)
    Page<JourneyAssignment> findEligibleAssignments(@Param("statuses") Collection<JourneyAssignmentStatus> statuses,
                                                    @Param("now") Instant now,
                                                    Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"journey", "journey.template", "nextStep", "lead"})
    @Query("select a from JourneyAssignment a where a.id = :id")
    Optional<JourneyAssignment> findByIdForUpdate(@Param("id") Long id);
}

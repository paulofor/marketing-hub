package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for assignments tying actors to journeys.
 */
public interface JourneyAssignmentRepository extends JpaRepository<JourneyAssignment, Long> {
    Page<JourneyAssignment> findByJourneyId(Long journeyId, Pageable pageable);

    Page<JourneyAssignment> findByJourneyIdAndStatus(Long journeyId, JourneyAssignmentStatus status, Pageable pageable);
}

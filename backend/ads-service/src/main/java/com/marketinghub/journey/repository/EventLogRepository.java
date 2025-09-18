package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository handling persisted journey events.
 */
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    long countByActorIdAndEventTypeAndOccurredAtAfter(UUID actorId, String eventType, Instant occurredAt);

    Optional<EventLog> findFirstByActorIdAndEventTypeOrderByOccurredAtDesc(UUID actorId, String eventType);
}

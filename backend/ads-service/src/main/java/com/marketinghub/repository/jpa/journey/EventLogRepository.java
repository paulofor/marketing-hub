package com.marketinghub.repository.jpa.journey;

import com.marketinghub.journey.model.EventLog;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository handling persisted journey events. */
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
  long countByActorIdAndEventTypeAndOccurredAtAfter(
      UUID actorId, String eventType, Instant occurredAt);

  Optional<EventLog> findFirstByActorIdAndEventTypeOrderByOccurredAtDesc(
      UUID actorId, String eventType);
}

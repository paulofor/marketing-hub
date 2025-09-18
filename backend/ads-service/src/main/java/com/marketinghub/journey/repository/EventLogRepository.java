package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository handling persisted journey events.
 */
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
}

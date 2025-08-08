package com.marketinghub.repository;

import com.marketinghub.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Outbox events.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByProcessedAtIsNull();

    @Query("select e from OutboxEvent e where e.createdAt between :start and :end")
    List<OutboxEvent> findByPeriod(@Param("start") Instant start,
                                   @Param("end") Instant end);
}

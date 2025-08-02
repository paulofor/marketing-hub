package com.example.marketinghub.service;

import com.example.marketinghub.model.OutboxEvent;
import com.example.marketinghub.repository.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically processes pending outbox events.
 */
@Component
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final GraphApiClient graphApiClient;

    public OutboxScheduler(OutboxRepository outboxRepository, GraphApiClient graphApiClient) {
        this.outboxRepository = outboxRepository;
        this.graphApiClient = graphApiClient;
    }

    /**
     * Re-sends events not yet processed.
     */
    @Scheduled(fixedRate = 60000)
    public void processPending() {
        List<OutboxEvent> events = outboxRepository.findByProcessedAtIsNull();
        for (OutboxEvent e : events) {
            graphApiClient.sendWelcomeAsync(null);
            e.setProcessedAt(Instant.now());
            outboxRepository.save(e);
        }
    }
}

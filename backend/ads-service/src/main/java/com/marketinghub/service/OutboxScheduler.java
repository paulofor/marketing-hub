package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.OutboxEvent;
import com.marketinghub.repository.OutboxRepository;
import com.marketinghub.repository.LeadRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Periodically processes pending outbox events.
 */
@Component
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final LeadRepository leadRepository;
    private final GraphApiClient graphApiClient;
    private final WelcomeSequenceFactory welcomeSequenceFactory;

    public OutboxScheduler(OutboxRepository outboxRepository,
                           LeadRepository leadRepository,
                           GraphApiClient graphApiClient,
                           WelcomeSequenceFactory welcomeSequenceFactory) {
        this.outboxRepository = outboxRepository;
        this.leadRepository = leadRepository;
        this.graphApiClient = graphApiClient;
        this.welcomeSequenceFactory = welcomeSequenceFactory;
    }

    /**
     * Re-sends events not yet processed.
     */
    @Scheduled(fixedRate = 60000)
    public void processPending() {
        List<OutboxEvent> events = outboxRepository.findByProcessedAtIsNull();
        for (OutboxEvent e : events) {
            if (e.getAggregateId() == null) {
                continue;
            }
            Optional<Lead> lead = leadRepository.findById(e.getAggregateId());
            if (lead.isEmpty()) {
                continue;
            }
            graphApiClient.sendWelcomeAsync(lead.get(), welcomeSequenceFactory.createWelcomeTemplate());
            e.setProcessedAt(Instant.now());
            outboxRepository.save(e);
        }
    }
}

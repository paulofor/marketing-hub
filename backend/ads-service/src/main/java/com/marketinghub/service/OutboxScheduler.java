package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.OutboxEvent;
import com.marketinghub.model.SequenceTemplate;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.core.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Periodically processes pending outbox events.
 */
@Component
public class OutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxRepository outboxRepository;
    private final LeadRepository leadRepository;
    private final GraphApiClient graphApiClient;
    private final WelcomeSequenceFactory welcomeSequenceFactory;
    private final int batchSize;

    public OutboxScheduler(OutboxRepository outboxRepository,
                           LeadRepository leadRepository,
                           GraphApiClient graphApiClient,
                           WelcomeSequenceFactory welcomeSequenceFactory,
                           @Value("${outbox.scheduler.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.leadRepository = leadRepository;
        this.graphApiClient = graphApiClient;
        this.welcomeSequenceFactory = welcomeSequenceFactory;
        this.batchSize = batchSize;
    }

    /**
     * Re-sends events not yet processed.
     */
    @Scheduled(fixedRate = 60000)
    public void processPending() {
        List<OutboxEvent> events = outboxRepository.findPending(PageRequest.of(0, batchSize));
        if (events.isEmpty()) {
            return;
        }
        Map<UUID, Lead> leads = loadLeads(events);
        SequenceTemplate welcomeTemplate = welcomeSequenceFactory.createWelcomeTemplate();
        int delivered = 0;
        for (OutboxEvent event : events) {
            Lead lead = leads.get(event.getAggregateId());
            if (lead == null) {
                log.warn("Lead {} referenced in outbox event {} was not found; marking event as processed",
                        event.getAggregateId(), event.getId());
                event.setProcessedAt(Instant.now());
                continue;
            }
            graphApiClient.sendWelcomeAsync(lead, welcomeTemplate);
            event.setProcessedAt(Instant.now());
            delivered++;
        }
        outboxRepository.saveAll(events);
        log.debug("Outbox scheduler processed {} events ({} deliveries)", events.size(), delivered);
    }

    private Map<UUID, Lead> loadLeads(List<OutboxEvent> events) {
        List<UUID> leadIds = events.stream()
                .map(OutboxEvent::getAggregateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (leadIds.isEmpty()) {
            return Map.of();
        }
        return leadRepository.findAllById(leadIds).stream()
                .collect(Collectors.toMap(Lead::getId, Function.identity()));
    }
}

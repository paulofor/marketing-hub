package com.marketinghub.service;

import com.marketinghub.dto.LeadDTO;
import com.marketinghub.model.*;
import com.marketinghub.repository.LeadRepository;
import com.marketinghub.repository.OutboxRepository;
import com.marketinghub.experiment.Experiment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles lead persistence and outbox creation.
 */
@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final OutboxRepository outboxRepository;
    private final GraphApiClient graphApiClient;
    private final TaskExecutor taskExecutor;
    private final WelcomeSequenceFactory welcomeSequenceFactory;

    public LeadService(LeadRepository leadRepository,
                       OutboxRepository outboxRepository,
                       GraphApiClient graphApiClient,
                       TaskExecutor taskExecutor,
                       WelcomeSequenceFactory welcomeSequenceFactory) {
        this.leadRepository = leadRepository;
        this.outboxRepository = outboxRepository;
        this.graphApiClient = graphApiClient;
        this.taskExecutor = taskExecutor;
        this.welcomeSequenceFactory = welcomeSequenceFactory;
    }

    /**
     * Saves a lead from webhook and enqueues an outbox event.
     *
     * @param dto incoming lead data
     * @return persisted lead
     */
    @Transactional
    public Lead saveFromWebhook(LeadDTO dto) {
        Lead lead = Lead.builder()
                .leadgenId(dto.leadgenId())
                .instagramUserId(dto.instagramUserId())
                .adId(dto.adId())
                .campaignId(dto.campaignId())
                .capturedAt(dto.capturedAt())
                .nurtureStage(dto.nurtureStage() != null ? dto.nurtureStage() : NurtureStage.NEW)
                .build();
        if (dto.experimentId() != null) {
            Experiment exp = new Experiment();
            exp.setId(dto.experimentId());
            lead.setExperiment(exp);
        }
        lead = leadRepository.save(lead);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(lead.getId())
                .eventType("LEAD_CREATED")
                .payload(String.format("{\"leadId\":\"%s\"}", lead.getId()))
                .createdAt(Instant.now())
                .build();
        outboxRepository.save(event);

        SequenceTemplate template = welcomeSequenceFactory.createWelcomeTemplate();

        taskExecutor.execute(() -> graphApiClient.sendWelcomeAsync(lead, template));
        return lead;
    }
}

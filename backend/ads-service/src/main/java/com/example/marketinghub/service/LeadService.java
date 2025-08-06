package com.example.marketinghub.service;

import com.example.marketinghub.dto.LeadDTO;
import com.example.marketinghub.model.*;
import com.example.marketinghub.repository.LeadRepository;
import com.example.marketinghub.repository.OutboxRepository;
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

    public LeadService(LeadRepository leadRepository,
                       OutboxRepository outboxRepository,
                       GraphApiClient graphApiClient,
                       TaskExecutor taskExecutor) {
        this.leadRepository = leadRepository;
        this.outboxRepository = outboxRepository;
        this.graphApiClient = graphApiClient;
        this.taskExecutor = taskExecutor;
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
        leadRepository.save(lead);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(lead.getId())
                .eventType("LEAD_CREATED")
                .payload("{}")
                .createdAt(Instant.now())
                .build();
        outboxRepository.save(event);

        taskExecutor.execute(() -> graphApiClient.sendWelcomeAsync(lead));
        return lead;
    }
}

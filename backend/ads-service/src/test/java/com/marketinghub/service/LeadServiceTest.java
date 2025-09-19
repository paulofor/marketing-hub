package com.marketinghub.service;

import com.marketinghub.dto.LeadDTO;
import com.marketinghub.model.Lead;
import com.marketinghub.model.NurtureStage;
import com.marketinghub.model.OutboxEvent;
import com.marketinghub.repository.LeadRepository;
import com.marketinghub.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {
    @Mock
    LeadRepository leadRepository;
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    GraphApiClient graphApiClient;

    TaskExecutor taskExecutor = Runnable::run;
    WelcomeSequenceFactory welcomeSequenceFactory;

    LeadService leadService;

    @BeforeEach
    void setUp() {
        welcomeSequenceFactory = new WelcomeSequenceFactory();
        leadService = new LeadService(leadRepository, outboxRepository, graphApiClient, taskExecutor, welcomeSequenceFactory);
    }

    @Test
    void saveFromWebhookPersistsEntitiesAndCallsGraph() {
        when(leadRepository.save(any())).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        LeadDTO dto = new LeadDTO(1L, 2L, 3L, 4L, null, Instant.now(), NurtureStage.NEW);
        Lead lead = leadService.saveFromWebhook(dto);

        verify(leadRepository).save(any());
        var eventCaptor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(eventCaptor.capture());
        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(lead.getId(), savedEvent.getAggregateId());
        assertEquals("{\"leadId\":\"" + lead.getId() + "\"}", savedEvent.getPayload());
        verify(graphApiClient).sendWelcomeAsync(any(), any());
        assertEquals(dto.leadgenId(), lead.getLeadgenId());
    }
}

package com.example.marketinghub.service;

import com.example.marketinghub.dto.LeadDTO;
import com.example.marketinghub.model.Lead;
import com.example.marketinghub.model.NurtureStage;
import com.example.marketinghub.repository.LeadRepository;
import com.example.marketinghub.repository.OutboxRepository;
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

    LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(leadRepository, outboxRepository, graphApiClient, taskExecutor);
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
        verify(outboxRepository).save(any());
        verify(graphApiClient).sendWelcomeAsync(any());
        assertEquals(dto.leadgenId(), lead.getLeadgenId());
    }
}

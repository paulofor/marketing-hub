package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.OutboxEvent;
import com.marketinghub.repository.LeadRepository;
import com.marketinghub.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    LeadRepository leadRepository;
    @Mock
    GraphApiClient graphApiClient;

    OutboxScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxScheduler(outboxRepository, leadRepository, graphApiClient, new WelcomeSequenceFactory());
    }

    @Test
    void processPendingMarksEventsProcessed() {
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder().id(leadId).build();
        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(leadId)
                .payload("{\"leadId\":\"" + leadId + "\"}")
                .build();
        when(outboxRepository.findByProcessedAtIsNull()).thenReturn(Collections.singletonList(event));
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processPending();

        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).save(event);
        verify(graphApiClient).sendWelcomeAsync(eq(lead), any());
    }
}

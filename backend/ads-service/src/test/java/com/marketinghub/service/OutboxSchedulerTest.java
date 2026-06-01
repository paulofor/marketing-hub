package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.OutboxEvent;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.core.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

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
        scheduler = new OutboxScheduler(outboxRepository, leadRepository, graphApiClient, new WelcomeSequenceFactory(), 10);
    }

    @Test
    void processPendingMarksEventsProcessed() {
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder().id(leadId).build();
        OutboxEvent event = OutboxEvent.builder()
                .id(123L)
                .aggregateId(leadId)
                .payload("{\"leadId\":\"" + leadId + "\"}")
                .build();
        when(outboxRepository.findPending(any())).thenReturn(Collections.singletonList(event));
        when(leadRepository.findAllById(any())).thenReturn(Collections.singletonList(lead));

        scheduler.processPending();

        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).saveAll(Collections.singletonList(event));
        verify(graphApiClient).sendWelcomeAsync(eq(lead), any());
    }

    @Test
    void processPendingMarksMissingLeadsAsProcessedWithoutSending() {
        UUID leadId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(456L)
                .aggregateId(leadId)
                .build();
        when(outboxRepository.findPending(any())).thenReturn(List.of(event));
        when(leadRepository.findAllById(any())).thenReturn(Collections.emptyList());

        scheduler.processPending();

        assertNotNull(event.getProcessedAt());
        verify(graphApiClient, never()).sendWelcomeAsync(any(), any());
        verify(outboxRepository).saveAll(Collections.singletonList(event));
    }
}

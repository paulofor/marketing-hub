package com.marketinghub.service;

import com.marketinghub.model.OutboxEvent;
import com.marketinghub.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    GraphApiClient graphApiClient;

    @InjectMocks
    OutboxScheduler scheduler;

    @Test
    void processPendingMarksEventsProcessed() {
        OutboxEvent event = OutboxEvent.builder().build();
        when(outboxRepository.findByProcessedAtIsNull()).thenReturn(Collections.singletonList(event));

        scheduler.processPending();

        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).save(event);
    }
}

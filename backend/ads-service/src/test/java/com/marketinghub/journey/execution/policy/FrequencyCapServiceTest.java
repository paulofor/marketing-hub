package com.marketinghub.journey.execution.policy;

import com.marketinghub.journey.execution.JourneyExecutionProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.repository.jpa.journey.EventLogRepository;
import com.marketinghub.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrequencyCapServiceTest {
    private EventLogRepository eventLogRepository;
    private JourneyExecutionProperties properties;
    private FrequencyCapService service;
    private Instant now;

    @BeforeEach
    void setup() {
        eventLogRepository = mock(EventLogRepository.class);
        properties = new JourneyExecutionProperties();
        service = new FrequencyCapService(eventLogRepository, properties);
        now = Instant.now();
    }

    @Test
    void allowsWhenDisabled() {
        properties.getFrequencyCap().setEnabled(false);
        JourneyAssignment assignment = assignmentWithLead(UUID.randomUUID());

        FrequencyCapResult result = service.evaluate(assignment, now);

        assertThat(result.blocked()).isFalse();
    }

    @Test
    void blocksWhenDailyLimitReached() {
        properties.getFrequencyCap().setPerDay(1);
        properties.getFrequencyCap().setCooldown(Duration.ofHours(1));
        UUID leadId = UUID.randomUUID();
        JourneyAssignment assignment = assignmentWithLead(leadId);
        when(eventLogRepository.countByActorIdAndEventTypeAndOccurredAtAfter(eq(leadId), anyString(), any()))
                .thenReturn(1L);

        FrequencyCapResult result = service.evaluate(assignment, now);

        assertThat(result.blocked()).isTrue();
        assertThat(result.nextAttemptAt()).isAfter(now);
        assertThat(result.metadata()).containsEntry("window", "daily");
    }

    @Test
    void blocksWhenWeeklyLimitReached() {
        properties.getFrequencyCap().setPerDay(5);
        properties.getFrequencyCap().setPerWeek(6);
        UUID leadId = UUID.randomUUID();
        JourneyAssignment assignment = assignmentWithLead(leadId);
        when(eventLogRepository.countByActorIdAndEventTypeAndOccurredAtAfter(eq(leadId), anyString(), any()))
                .thenReturn(0L, 6L);

        FrequencyCapResult result = service.evaluate(assignment, now);

        assertThat(result.blocked()).isTrue();
        assertThat(result.metadata()).containsEntry("window", "weekly");
    }

    private JourneyAssignment assignmentWithLead(UUID leadId) {
        return JourneyAssignment.builder()
                .lead(Lead.builder().id(leadId).build())
                .build();
    }
}


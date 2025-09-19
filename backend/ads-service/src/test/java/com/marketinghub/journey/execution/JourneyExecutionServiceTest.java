package com.marketinghub.journey.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.JourneyChannelHandler;
import com.marketinghub.journey.execution.policy.FrequencyCapResult;
import com.marketinghub.journey.execution.policy.FrequencyCapService;
import com.marketinghub.journey.execution.policy.RetryBackoffCalculator;
import com.marketinghub.journey.model.*;
import com.marketinghub.journey.model.EventLog;
import com.marketinghub.journey.repository.EventLogRepository;
import com.marketinghub.journey.repository.JourneyAssignmentRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyExecutionServiceTest {
    @Mock
    private JourneyAssignmentRepository assignmentRepository;
    @Mock
    private JourneyStepRepository stepRepository;
    @Mock
    private EventLogRepository eventLogRepository;
    @Mock
    private FrequencyCapService frequencyCapService;
    @Mock
    private RetryBackoffCalculator retryBackoffCalculator;
    @Mock
    private TelemetryService telemetryService;
    @Mock
    private JourneyChannelHandler emailHandler;

    private JourneyExecutionService service;
    private JourneyExecutionProperties properties;
    private JourneyConditionEvaluator conditionEvaluator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        properties = new JourneyExecutionProperties();
        conditionEvaluator = new JourneyConditionEvaluator();
        objectMapper = new ObjectMapper();
        when(emailHandler.supportedType()).thenReturn(JourneyStimulusType.EMAIL);
        service = new JourneyExecutionService(assignmentRepository,
                stepRepository,
                eventLogRepository,
                properties,
                frequencyCapService,
                retryBackoffCalculator,
                conditionEvaluator,
                telemetryService,
                objectMapper,
                new NoOpTransactionManager(),
                List.of(emailHandler));
    }

    @Test
    void processDueAssignmentsDispatchesStep() {
        JourneyAssignment assignment = buildAssignment(JourneyStimulusType.EMAIL);
        when(assignmentRepository.findEligibleAssignments(anyList(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        when(assignmentRepository.findByIdForUpdate(assignment.getId())).thenReturn(Optional.of(assignment));
        when(frequencyCapService.evaluate(eq(assignment), any())).thenReturn(FrequencyCapResult.allow());
        when(stepRepository.findFirstByTemplateAndPositionGreaterThanOrderByPositionAsc(any(), anyInt()))
                .thenReturn(Optional.empty());
        when(emailHandler.dispatch(eq(assignment), eq(assignment.getNextStep()), anyMap()))
                .thenReturn(ChannelDispatchResult.success("provider-123", Map.of("value", BigDecimal.TEN)));

        service.processDueAssignments();

        assertThat(assignment.getStatus()).isEqualTo(JourneyAssignmentStatus.COMPLETED);
        assertThat(assignment.getNextStep()).isNull();
        assertThat(assignment.getRetryCount()).isZero();
        verify(telemetryService).emitStepDispatched(eq(assignment), eq(assignment.getCurrentStep()), anyMap(), argThat(m -> m.containsKey("providerMessageId")));

        ArgumentCaptor<EventLog> eventCaptor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository, atLeastOnce()).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(EventLog::getEventType)
                .contains(JourneyEventType.STIMULUS_DISPATCHED.getCode(), JourneyEventType.JOURNEY_COMPLETED.getCode());
    }

    @Test
    void processDueAssignmentsHonoursFrequencyCap() {
        JourneyAssignment assignment = buildAssignment(JourneyStimulusType.EMAIL);
        Instant nextAttempt = Instant.now().plus(Duration.ofHours(6));
        when(assignmentRepository.findEligibleAssignments(anyList(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        when(assignmentRepository.findByIdForUpdate(assignment.getId())).thenReturn(Optional.of(assignment));
        when(frequencyCapService.evaluate(eq(assignment), any()))
                .thenReturn(FrequencyCapResult.block(nextAttempt, Map.of("window", "daily")));

        service.processDueAssignments();

        assertThat(assignment.getNextAttemptAt()).isEqualTo(nextAttempt);
        assertThat(assignment.getStatus()).isEqualTo(JourneyAssignmentStatus.IN_PROGRESS);
        verify(emailHandler, never()).dispatch(any(), any(), anyMap());
        verify(eventLogRepository).save(argThat(log -> log.getEventType().equals(JourneyEventType.STIMULUS_FREQUENCY_CAPPED.getCode())));
    }

    @Test
    void processDueAssignmentsSchedulesRetryOnTransientFailure() {
        JourneyAssignment assignment = buildAssignment(JourneyStimulusType.EMAIL);
        when(assignmentRepository.findEligibleAssignments(anyList(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        when(assignmentRepository.findByIdForUpdate(assignment.getId())).thenReturn(Optional.of(assignment));
        when(frequencyCapService.evaluate(eq(assignment), any())).thenReturn(FrequencyCapResult.allow());
        when(emailHandler.dispatch(eq(assignment), eq(assignment.getNextStep()), anyMap()))
                .thenReturn(ChannelDispatchResult.transientFailure("rate_limited", null, Map.of()));
        when(retryBackoffCalculator.computeDelay(eq(1))).thenReturn(Duration.ofMinutes(5));

        service.processDueAssignments();

        assertThat(assignment.getStatus()).isEqualTo(JourneyAssignmentStatus.IN_PROGRESS);
        assertThat(assignment.getRetryCount()).isEqualTo(1);
        assertThat(assignment.getNextAttemptAt()).isNotNull();
        verify(eventLogRepository).save(argThat(log -> log.getEventType().equals(JourneyEventType.STIMULUS_FAILED.getCode())));
    }

    @Test
    void processDueAssignmentsStopsAfterPermanentFailure() {
        JourneyAssignment assignment = buildAssignment(JourneyStimulusType.EMAIL);
        when(assignmentRepository.findEligibleAssignments(anyList(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        when(assignmentRepository.findByIdForUpdate(assignment.getId())).thenReturn(Optional.of(assignment));
        when(frequencyCapService.evaluate(eq(assignment), any())).thenReturn(FrequencyCapResult.allow());
        when(emailHandler.dispatch(eq(assignment), eq(assignment.getNextStep()), anyMap()))
                .thenReturn(ChannelDispatchResult.permanentFailure("invalid_payload", Map.of()));

        service.processDueAssignments();

        assertThat(assignment.getStatus()).isEqualTo(JourneyAssignmentStatus.STOPPED);
        assertThat(assignment.getNextAttemptAt()).isNull();
        verify(eventLogRepository).save(argThat(log -> log.getEventType().equals(JourneyEventType.STIMULUS_FAILED.getCode())));
    }

    private JourneyAssignment buildAssignment(JourneyStimulusType stimulusType) {
        JourneyTemplate template = JourneyTemplate.builder()
                .id(10L)
                .name("Template")
                .build();
        Journey journey = Journey.builder()
                .id(22L)
                .template(template)
                .name("Journey")
                .status(JourneyStatus.ACTIVE)
                .startAt(Instant.now().minus(Duration.ofHours(1)))
                .build();
        JourneyStep step = JourneyStep.builder()
                .id(33L)
                .template(template)
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(stimulusType)
                .delayMinutes(0)
                .name("Email welcome")
                .build();
        template.getSteps().add(step);
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(44L)
                .journey(journey)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .status(JourneyAssignmentStatus.PENDING)
                .nextStep(step)
                .build();
        assignment.setCreatedAt(Instant.now().minus(Duration.ofHours(2)));
        return assignment;
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            // no-op
        }

        @Override
        public void rollback(TransactionStatus status) {
            // no-op
        }
    }
}

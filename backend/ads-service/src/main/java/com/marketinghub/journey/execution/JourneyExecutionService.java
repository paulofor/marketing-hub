package com.marketinghub.journey.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.ChannelDispatchStatus;
import com.marketinghub.journey.execution.channel.JourneyChannelHandler;
import com.marketinghub.journey.execution.policy.FrequencyCapResult;
import com.marketinghub.journey.execution.policy.FrequencyCapService;
import com.marketinghub.journey.execution.policy.RetryBackoffCalculator;
import com.marketinghub.journey.model.*;
import com.marketinghub.journey.repository.EventLogRepository;
import com.marketinghub.journey.repository.JourneyAssignmentRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.model.Lead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core orchestrator that transforms journey models into actionable stimuli.
 */
@Service
@Slf4j
public class JourneyExecutionService {
    private final JourneyAssignmentRepository assignmentRepository;
    private final JourneyStepRepository stepRepository;
    private final EventLogRepository eventLogRepository;
    private final JourneyExecutionProperties properties;
    private final FrequencyCapService frequencyCapService;
    private final RetryBackoffCalculator retryBackoffCalculator;
    private final JourneyConditionEvaluator conditionEvaluator;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Map<JourneyStimulusType, JourneyChannelHandler> handlerByType;

    public JourneyExecutionService(JourneyAssignmentRepository assignmentRepository,
                                   JourneyStepRepository stepRepository,
                                   EventLogRepository eventLogRepository,
                                   JourneyExecutionProperties properties,
                                   FrequencyCapService frequencyCapService,
                                   RetryBackoffCalculator retryBackoffCalculator,
                                   JourneyConditionEvaluator conditionEvaluator,
                                   TelemetryService telemetryService,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager transactionManager,
                                   List<JourneyChannelHandler> handlers) {
        this.assignmentRepository = assignmentRepository;
        this.stepRepository = stepRepository;
        this.eventLogRepository = eventLogRepository;
        this.properties = properties;
        this.frequencyCapService = frequencyCapService;
        this.retryBackoffCalculator = retryBackoffCalculator;
        this.conditionEvaluator = conditionEvaluator;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.handlerByType = handlers.stream()
                .collect(Collectors.toMap(JourneyChannelHandler::supportedType, h -> h));
    }

    public void processDueAssignments() {
        Instant now = Instant.now();
        List<JourneyAssignment> candidates = assignmentRepository
                .findEligibleAssignments(List.of(JourneyAssignmentStatus.PENDING, JourneyAssignmentStatus.IN_PROGRESS),
                        now,
                        PageRequest.of(0, properties.getBatchSize()))
                .getContent();
        for (JourneyAssignment assignment : candidates) {
            transactionTemplate.executeWithoutResult(status -> processSingle(assignment.getId(), now));
        }
    }

    private void processSingle(Long assignmentId, Instant referenceTime) {
        Optional<JourneyAssignment> optional = assignmentRepository.findByIdForUpdate(assignmentId);
        if (optional.isEmpty()) {
            return;
        }
        JourneyAssignment assignment = optional.get();
        JourneyStep step = assignment.getNextStep();
        if (step == null) {
            markJourneyCompleteIfNeeded(assignment, referenceTime);
            assignmentRepository.save(assignment);
            return;
        }
        if (!isJourneyActive(assignment.getJourney(), referenceTime)) {
            return;
        }
        if (!isDue(assignment, step, referenceTime)) {
            return;
        }

        Map<String, Object> context = parseContext(assignment.getContextPayload());
        assignment.setStatus(JourneyAssignmentStatus.IN_PROGRESS);

        if (!conditionEvaluator.evaluateEntryCondition(assignment, step, context)) {
            skipStep(assignment, step, context, referenceTime, "entry_condition_false");
            return;
        }

        FrequencyCapResult capResult = frequencyCapService.evaluate(assignment, referenceTime);
        if (capResult.blocked()) {
            handleFrequencyCap(assignment, step, referenceTime, capResult);
            return;
        }

        JourneyChannelHandler handler = handlerByType.get(step.getStimulusType());
        if (handler == null) {
            log.warn("No handler registered for stimulus type {}", step.getStimulusType());
            handlePermanentFailure(assignment, step, referenceTime,
                    ChannelDispatchResult.permanentFailure("No handler for stimulus type", Map.of("stimulusType", step.getStimulusType().name())));
            return;
        }

        ChannelDispatchResult dispatchResult;
        try {
            dispatchResult = handler.dispatch(assignment, step, context);
        } catch (Exception ex) {
            log.error("Unexpected error dispatching assignment {}", assignment.getId(), ex);
            dispatchResult = ChannelDispatchResult.transientFailure("Unexpected error: " + ex.getMessage(), null, Map.of());
        }
        applyDispatchResult(assignment, step, context, referenceTime, dispatchResult);
    }

    private boolean isJourneyActive(Journey journey, Instant now) {
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            return false;
        }
        if (journey.getStartAt() != null && now.isBefore(journey.getStartAt())) {
            return false;
        }
        return journey.getEndAt() == null || !now.isAfter(journey.getEndAt());
    }

    private boolean isDue(JourneyAssignment assignment, JourneyStep step, Instant now) {
        Instant candidate = assignment.getNextAttemptAt();
        if (candidate == null) {
            Instant base = assignment.getLastEventAt();
            if (base == null) {
                base = assignment.getCreatedAt();
            }
            Instant journeyStart = assignment.getJourney().getStartAt();
            if (journeyStart != null && (base == null || base.isBefore(journeyStart))) {
                base = journeyStart;
            }
            if (base == null) {
                base = now;
            }
            int delayMinutes = step.getDelayMinutes() != null ? step.getDelayMinutes() : 0;
            candidate = base.plus(Duration.ofMinutes(delayMinutes));
        }
        return !candidate.isAfter(now);
    }

    private void skipStep(JourneyAssignment assignment,
                          JourneyStep step,
                          Map<String, Object> context,
                          Instant occurredAt,
                          String reason) {
        log.debug("Skipping step {} for assignment {}", step.getId(), assignment.getId());
        JourneyStep next = resolveNextStep(step);
        assignment.setCurrentStep(step);
        assignment.setLastEventAt(occurredAt);
        assignment.setNextStep(next);
        assignment.setRetryCount(0);
        assignment.setNextAttemptAt(null);
        assignment.setStatus(next == null ? JourneyAssignmentStatus.COMPLETED : JourneyAssignmentStatus.IN_PROGRESS);
        assignmentRepository.save(assignment);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("stepId", step.getId());
        metadata.put("context", context);
        recordEvent(assignment, step, occurredAt, JourneyEventType.STIMULUS_SKIPPED, metadata);
        if (next == null) {
            recordEvent(assignment, step, occurredAt, JourneyEventType.JOURNEY_COMPLETED, Map.of());
        }
    }

    private void handleFrequencyCap(JourneyAssignment assignment,
                                    JourneyStep step,
                                    Instant now,
                                    FrequencyCapResult capResult) {
        assignment.setNextAttemptAt(capResult.nextAttemptAt());
        assignment.setRetryCount(0);
        assignment.setStatus(JourneyAssignmentStatus.IN_PROGRESS);
        assignmentRepository.save(assignment);
        recordEvent(assignment, step, now, JourneyEventType.STIMULUS_FREQUENCY_CAPPED, capResult.metadata());
    }

    private void applyDispatchResult(JourneyAssignment assignment,
                                     JourneyStep step,
                                     Map<String, Object> context,
                                     Instant occurredAt,
                                     ChannelDispatchResult result) {
        if (result.status() == ChannelDispatchStatus.OK) {
            handleSuccess(assignment, step, context, occurredAt, result);
            return;
        }
        if (result.status() == ChannelDispatchStatus.TRANSIENT_ERROR) {
            handleTransientFailure(assignment, step, occurredAt, result);
            return;
        }
        handlePermanentFailure(assignment, step, occurredAt, result);
    }

    private void handleSuccess(JourneyAssignment assignment,
                                JourneyStep step,
                                Map<String, Object> context,
                                Instant occurredAt,
                                ChannelDispatchResult result) {
        JourneyStep next = resolveNextStep(step);
        assignment.setCurrentStep(step);
        assignment.setLastEventAt(occurredAt);
        assignment.setNextStep(next);
        assignment.setNextAttemptAt(null);
        assignment.setRetryCount(0);
        assignment.setStatus(next == null ? JourneyAssignmentStatus.COMPLETED : JourneyAssignmentStatus.IN_PROGRESS);
        assignmentRepository.save(assignment);

        Map<String, Object> metadata = new HashMap<>(result.metadata());
        if (result.providerMessageId() != null) {
            metadata.put("providerMessageId", result.providerMessageId());
        }
        metadata.put("stepId", step.getId());
        recordEvent(assignment, step, occurredAt, JourneyEventType.STIMULUS_DISPATCHED, metadata);
        telemetryService.emitStepDispatched(assignment, step, context, metadata);
        if (next == null) {
            recordEvent(assignment, step, occurredAt, JourneyEventType.JOURNEY_COMPLETED, Map.of());
        }
    }

    private void handleTransientFailure(JourneyAssignment assignment,
                                        JourneyStep step,
                                        Instant now,
                                        ChannelDispatchResult result) {
        int attempt = assignment.getRetryCount() != null ? assignment.getRetryCount() + 1 : 1;
        assignment.setRetryCount(attempt);
        if (attempt >= properties.getRetry().getMaxAttempts()) {
            log.warn("Max retry attempts reached for assignment {}", assignment.getId());
            assignment.setStatus(JourneyAssignmentStatus.STOPPED);
            assignment.setNextAttemptAt(null);
            assignmentRepository.save(assignment);
            Map<String, Object> metadata = new HashMap<>(result.metadata());
            metadata.put("maxRetries", properties.getRetry().getMaxAttempts());
            metadata.put("error", result.errorMessage());
            recordEvent(assignment, step, now, JourneyEventType.STIMULUS_FAILED, metadata);
            return;
        }
        Instant nextAttempt = result.nextAttemptAt();
        if (nextAttempt == null) {
            Duration backoff = retryBackoffCalculator.computeDelay(attempt);
            nextAttempt = now.plus(backoff);
        }
        assignment.setNextAttemptAt(nextAttempt);
        assignment.setStatus(JourneyAssignmentStatus.IN_PROGRESS);
        assignmentRepository.save(assignment);
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("retryCount", attempt);
        metadata.put("nextAttemptAt", nextAttempt);
        metadata.put("error", result.errorMessage());
        recordEvent(assignment, step, now, JourneyEventType.STIMULUS_FAILED, metadata);
    }

    private void handlePermanentFailure(JourneyAssignment assignment,
                                        JourneyStep step,
                                        Instant now,
                                        ChannelDispatchResult result) {
        assignment.setStatus(JourneyAssignmentStatus.STOPPED);
        assignment.setNextAttemptAt(null);
        assignmentRepository.save(assignment);
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("error", result.errorMessage());
        recordEvent(assignment, step, now, JourneyEventType.STIMULUS_FAILED, metadata);
    }

    private void markJourneyCompleteIfNeeded(JourneyAssignment assignment, Instant now) {
        if (assignment.getStatus() != JourneyAssignmentStatus.COMPLETED) {
            assignment.setStatus(JourneyAssignmentStatus.COMPLETED);
            recordEvent(assignment, null, now, JourneyEventType.JOURNEY_COMPLETED, Map.of());
        }
    }

    private JourneyStep resolveNextStep(JourneyStep current) {
        return stepRepository.findFirstByTemplateAndPositionGreaterThanOrderByPositionAsc(current.getTemplate(), current.getPosition())
                .orElse(null);
    }

    private Map<String, Object> parseContext(String contextPayload) {
        if (contextPayload == null || contextPayload.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(contextPayload, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid context payload for journey assignment", e);
            return new HashMap<>();
        }
    }

    private void recordEvent(JourneyAssignment assignment,
                              JourneyStep step,
                              Instant occurredAt,
                              JourneyEventType type,
                              Map<String, Object> metadata) {
        EventLog logEntry = EventLog.builder()
                .actorId(extractActorId(assignment.getLead()))
                .eventType(type.getCode())
                .journey(assignment.getJourney())
                .journeyStep(step)
                .source("journey-orchestrator")
                .metadata(toJson(metadata))
                .occurredAt(occurredAt)
                .build();
        eventLogRepository.save(logEntry);
    }

    private UUID extractActorId(Lead lead) {
        if (lead == null) {
            return null;
        }
        return lead.getId();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise metadata", e);
            return null;
        }
    }
}

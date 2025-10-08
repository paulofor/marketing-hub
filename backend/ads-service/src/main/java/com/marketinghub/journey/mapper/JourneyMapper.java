package com.marketinghub.journey.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.dto.*;
import com.marketinghub.journey.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Mapper centralizing conversions between domain entities and API DTOs.
 */
@Component
public class JourneyMapper {
    private final ObjectMapper objectMapper;

    public JourneyMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JourneyTemplateSummaryResponse toSummary(JourneyTemplate template) {
        return new JourneyTemplateSummaryResponse(
                template.getId(),
                template.getName(),
                template.getObjective(),
                copyPhases(template.getPhases()),
                template.getPreferredChannel(),
                copyTags(template.getTags()),
                copyMetadata(template.getMetadata()),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public JourneyTemplateResponse toResponse(JourneyTemplate template) {
        List<JourneyStepResponse> steps = template.getSteps().stream()
                .sorted(Comparator.comparing(JourneyStep::getPosition, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toStepResponse)
                .toList();
        return new JourneyTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getObjective(),
                copyPhases(template.getPhases()),
                template.getPreferredChannel(),
                copyTags(template.getTags()),
                copyMetadata(template.getMetadata()),
                steps,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public JourneyStepResponse toStepResponse(JourneyStep step) {
        return new JourneyStepResponse(
                step.getId(),
                step.getTemplate().getId(),
                step.getPosition(),
                step.getName(),
                step.getDescription(),
                step.getPhase(),
                step.getStimulusType(),
                step.getCreative() != null ? step.getCreative().getId() : null,
                step.getAngle() != null ? step.getAngle().getId() : null,
                step.getVisualProof() != null ? step.getVisualProof().getId() : null,
                step.getEmotionalTrigger() != null ? step.getEmotionalTrigger().getId() : null,
                step.getEntryCondition(),
                step.getExitCondition(),
                step.getDelayMinutes(),
                copyMetadata(step.getMetadata())
        );
    }

    private List<JourneyPhase> copyPhases(List<JourneyPhase> phases) {
        if (phases == null || phases.isEmpty()) {
            return List.of();
        }
        return phases.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<String> copyTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private Map<String, String> copyMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null) {
                sanitized.put(key, value);
            }
        });
        return new LinkedHashMap<>(sanitized);
    }

    public JourneyResponse toJourneyResponse(Journey journey) {
        return new JourneyResponse(
                journey.getId(),
                journey.getTemplate().getId(),
                journey.getTemplate().getName(),
                journey.getName(),
                journey.getDescription(),
                journey.getStatus(),
                journey.getMarketNiche() != null ? journey.getMarketNiche().getId() : null,
                journey.getExperiment() != null ? journey.getExperiment().getId() : null,
                journey.getSegmentReference(),
                journey.getSegmentFilter(),
                new LinkedHashMap<>(journey.getMetadata()),
                journey.getStartAt(),
                journey.getEndAt(),
                journey.getCreatedAt(),
                journey.getUpdatedAt()
        );
    }

    public JourneyAssignmentResponse toAssignmentResponse(JourneyAssignment assignment) {
        return new JourneyAssignmentResponse(
                assignment.getId(),
                assignment.getJourney().getId(),
                assignment.getType(),
                assignment.getStatus(),
                assignment.getLead() != null ? assignment.getLead().getId() : null,
                assignment.getSegmentIdentifier(),
                assignment.getCurrentStep() != null ? assignment.getCurrentStep().getId() : null,
                assignment.getNextStep() != null ? assignment.getNextStep().getId() : null,
                assignment.getLastEventAt(),
                assignment.getContextPayload(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    public EventLogResponse toEventLogResponse(EventLog log) {
        return new EventLogResponse(
                log.getId(),
                log.getActorId(),
                log.getEventType(),
                log.getJourney() != null ? log.getJourney().getId() : null,
                log.getJourneyStep() != null ? log.getJourneyStep().getId() : null,
                log.getSource(),
                log.getCampaignId(),
                deserializeMetadata(log.getMetadata()),
                log.getValue(),
                log.getOccurredAt(),
                log.getReceivedAt()
        );
    }

    private Map<String, Object> deserializeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, Map.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse event metadata", e);
        }
    }
}

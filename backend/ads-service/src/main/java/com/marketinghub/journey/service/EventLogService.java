package com.marketinghub.journey.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.dto.EventLogRequest;
import com.marketinghub.journey.model.EventLog;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.repository.EventLogRepository;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * Service capturing canonical event logs for journeys.
 */
@Service
public class EventLogService {
    private final EventLogRepository eventLogRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyStepRepository journeyStepRepository;
    private final ObjectMapper objectMapper;

    public EventLogService(EventLogRepository eventLogRepository,
                           JourneyRepository journeyRepository,
                           JourneyStepRepository journeyStepRepository,
                           ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.journeyRepository = journeyRepository;
        this.journeyStepRepository = journeyStepRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventLog record(EventLogRequest request) {
        Journey journey = null;
        if (request.journeyId() != null) {
            journey = journeyRepository.findById(request.journeyId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found"));
        }

        JourneyStep journeyStep = null;
        if (request.journeyStepId() != null) {
            journeyStep = journeyStepRepository.findById(request.journeyStepId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));
            if (journey != null && !journeyStep.getTemplate().getId().equals(journey.getTemplate().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step does not belong to journey template");
            }
        }

        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        String metadata = serializeMetadata(request.metadata());

        EventLog log = EventLog.builder()
                .actorId(request.actorId())
                .eventType(request.eventType())
                .journey(journey)
                .journeyStep(journeyStep)
                .source(request.source())
                .campaignId(request.campaignId())
                .metadata(metadata)
                .value(request.value())
                .occurredAt(occurredAt)
                .build();

        return eventLogRepository.save(log);
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata payload", e);
        }
    }
}

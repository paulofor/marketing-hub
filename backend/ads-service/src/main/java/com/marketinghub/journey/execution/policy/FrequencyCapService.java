package com.marketinghub.journey.execution.policy;

import com.marketinghub.journey.execution.JourneyExecutionProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyEventType;
import com.marketinghub.repository.jpa.journey.EventLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies exposure limits per actor to avoid over stimulation.
 */
@Service
@Slf4j
public class FrequencyCapService {
    private final EventLogRepository eventLogRepository;
    private final JourneyExecutionProperties properties;

    public FrequencyCapService(EventLogRepository eventLogRepository,
                               JourneyExecutionProperties properties) {
        this.eventLogRepository = eventLogRepository;
        this.properties = properties;
    }

    public FrequencyCapResult evaluate(JourneyAssignment assignment, Instant now) {
        JourneyExecutionProperties.FrequencyCapProperties cap = properties.getFrequencyCap();
        if (!cap.isEnabled()) {
            return FrequencyCapResult.allow();
        }
        if (assignment.getLead() == null || assignment.getLead().getId() == null) {
            return FrequencyCapResult.allow();
        }
        UUID actorId = assignment.getLead().getId();
        String eventType = JourneyEventType.STIMULUS_DISPATCHED.getCode();

        if (cap.getPerDay() > 0) {
            Instant dayStart = now.minus(Duration.ofDays(1));
            long dailyCount = eventLogRepository.countByActorIdAndEventTypeAndOccurredAtAfter(actorId, eventType, dayStart);
            if (dailyCount >= cap.getPerDay()) {
                return block(actorId, "daily", cap.getPerDay(), dailyCount, now, cap);
            }
        }
        if (cap.getPerWeek() > 0) {
            Instant weekStart = now.minus(Duration.ofDays(7));
            long weeklyCount = eventLogRepository.countByActorIdAndEventTypeAndOccurredAtAfter(actorId, eventType, weekStart);
            if (weeklyCount >= cap.getPerWeek()) {
                return block(actorId, "weekly", cap.getPerWeek(), weeklyCount, now, cap);
            }
        }
        return FrequencyCapResult.allow();
    }

    private FrequencyCapResult block(UUID actorId,
                                     String window,
                                     int limit,
                                     long current,
                                     Instant now,
                                     JourneyExecutionProperties.FrequencyCapProperties cap) {
        Instant nextAttemptAt = now.plus(cap.getCooldown());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actorId", actorId);
        metadata.put("window", window);
        metadata.put("limit", limit);
        metadata.put("current", current);
        metadata.put("cooldownSeconds", cap.getCooldown().toSeconds());
        log.debug("Frequency cap reached for actor {} on {} window", actorId, window);
        return FrequencyCapResult.block(nextAttemptAt, metadata);
    }
}

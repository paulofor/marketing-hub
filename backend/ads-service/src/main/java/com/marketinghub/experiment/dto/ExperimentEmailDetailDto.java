package com.marketinghub.experiment.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Detailed information about the email planned for a specific journey step.
 */
public record ExperimentEmailDetailDto(
        Long experimentId,
        Long journeyId,
        Long stepId,
        String stepName,
        Integer stepPosition,
        String stepPhase,
        String stepDescription,
        Map<String, String> stepMetadata,
        String subject,
        String templateId,
        String status,
        String notes,
        String preheader,
        String model,
        String prompt,
        boolean approved,
        Instant journeyCreatedAt,
        Instant journeyUpdatedAt
) {
}

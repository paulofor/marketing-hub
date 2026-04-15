package com.marketinghub.oprm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmFeedbackPublishRequestDto(
        @NotBlank String jobId,
        @NotBlank String correlationId,
        @NotBlank String occupationName,
        @NotBlank String personaLabel,
        @NotBlank String baselineRoutineArtifactId,
        @NotBlank String baselineFrameworkArtifactId,
        @NotNull Map<String, Object> recalibratedPainSignals,
        @NotNull Map<String, Object> recalibratedMechanismSignals,
        @NotNull Map<String, Object> hypothesisComparison,
        @NotNull Map<String, Object> scoreReweighting,
        @NotBlank String generatedAt
) {
}

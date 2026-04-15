package com.marketinghub.oprm.integration.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmFeedbackPublishRequest(
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

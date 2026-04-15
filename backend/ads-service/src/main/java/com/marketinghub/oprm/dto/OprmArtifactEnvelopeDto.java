package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmArtifactStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record OprmArtifactEnvelopeDto(
        @NotBlank String artifactType,
        @NotBlank String artifactVersion,
        @NotBlank String artifactId,
        @NotBlank String moduleName,
        @NotBlank String producer,
        @NotBlank String createdAt,
        @NotBlank String correlationId,
        @NotBlank String traceId,
        @NotNull List<String> sourceRefs,
        @NotNull List<String> inputRefs,
        @NotNull Map<String, Object> payload,
        @NotNull OprmArtifactStatus status,
        Double confidenceScore,
        @NotNull Map<String, Object> metadata
) {
}

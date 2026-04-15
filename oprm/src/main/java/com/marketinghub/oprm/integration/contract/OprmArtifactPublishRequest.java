package com.marketinghub.oprm.integration.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmArtifactPublishRequest(
        @NotBlank String jobId,
        @NotBlank String correlationId,
        @NotNull @Valid OprmArtifactEnvelopeDto artifact,
        @NotNull Map<String, Object> lineage,
        @NotBlank String idempotencyKey
) {
}

package com.marketinghub.mds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MdsRequestCreateRequest(
        @NotBlank String market,
        @NotBlank String problem,
        @NotBlank String desiredOutcome,
        @NotNull Map<String, Object> context,
        String deliveryConstraint,
        String evidencePreference,
        @NotBlank String correlationId
) {
}

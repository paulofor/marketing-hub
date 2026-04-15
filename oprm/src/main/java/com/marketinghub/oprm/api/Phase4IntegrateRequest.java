package com.marketinghub.oprm.api;

import jakarta.validation.constraints.NotBlank;

public record Phase4IntegrateRequest(
        @NotBlank String occupationLabel,
        @NotBlank String nicheName,
        @NotBlank String locale,
        String correlationId) {
}

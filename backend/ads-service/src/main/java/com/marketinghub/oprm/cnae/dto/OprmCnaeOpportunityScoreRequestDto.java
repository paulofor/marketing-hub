package com.marketinghub.oprm.cnae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de gravação do score de oportunidade calculado pelo módulo OPRM.
 */
public record OprmCnaeOpportunityScoreRequestDto(
        @NotBlank String cnaeDescription,
        @NotNull BigDecimal opportunityScore,
        @NotNull BigDecimal marketVolumeScore,
        @NotNull BigDecimal meiDensityScore,
        @NotNull BigDecimal digitalFitScore,
        @NotNull BigDecimal painClarityScore,
        String scoreJustification,
        @NotBlank String algorithmVersion,
        @NotBlank String cycleId,
        @NotNull Instant scoredAt,
        @NotBlank String scoreStatus) {}

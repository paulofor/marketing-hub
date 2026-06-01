package com.marketinghub.oprm.cnae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de gravação de candidato de nicho produzido pelo enriquecimento OPRM.
 */
public record OprmNicheCandidateRequestDto(
        @NotBlank String cnaeCode,
        @NotBlank String cnaeDescription,
        @NotBlank String candidateNicheName,
        String persona,
        String painHypothesis,
        String desiredOutcome,
        String mechanismHypothesis,
        String proofDirection,
        String offerIdea,
        String marketVolumeSignals,
        @NotNull BigDecimal opportunityScore,
        @NotBlank String scoreCycleId,
        @NotBlank String enrichmentCycleId,
        @NotBlank String status,
        String sourceArtifacts) {}

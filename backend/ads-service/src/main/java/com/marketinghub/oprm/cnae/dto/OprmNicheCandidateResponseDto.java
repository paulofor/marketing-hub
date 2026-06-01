package com.marketinghub.oprm.cnae.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resposta para candidato de nicho persistido a partir de um CNAE enriquecido.
 */
public record OprmNicheCandidateResponseDto(
        Long id,
        String cnaeCode,
        String cnaeDescription,
        String candidateNicheName,
        String persona,
        String painHypothesis,
        String desiredOutcome,
        String mechanismHypothesis,
        String proofDirection,
        String offerIdea,
        String marketVolumeSignals,
        BigDecimal opportunityScore,
        String scoreCycleId,
        String enrichmentCycleId,
        String status,
        String sourceArtifacts,
        Long marketNicheId,
        Instant createdAt,
        Instant updatedAt) {}

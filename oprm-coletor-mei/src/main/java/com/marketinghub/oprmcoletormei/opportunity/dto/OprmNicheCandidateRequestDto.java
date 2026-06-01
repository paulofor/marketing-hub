package com.marketinghub.oprmcoletormei.opportunity.dto;

import java.math.BigDecimal;

/** DTO usado pelo OPRM para publicar candidato de nicho derivado de CNAE enriquecido. */
public record OprmNicheCandidateRequestDto(
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
        String sourceArtifacts) {}

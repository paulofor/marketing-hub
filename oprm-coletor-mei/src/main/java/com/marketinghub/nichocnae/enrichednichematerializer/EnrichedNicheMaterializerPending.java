package com.marketinghub.nichocnae.enrichednichematerializer;

import java.math.BigDecimal;
import java.time.Instant;

/** Unidade de trabalho da etapa final recebida do backend para materializar nicho enriquecido. */
public record EnrichedNicheMaterializerPending(
        Long routineCardId,
        Long researchCycleId,
        Long sourceNicheCandidateId,
        Long existingMarketNicheId,
        String cnaeCode,
        String cnaeDescription,
        String originalNicheName,
        String nicheName,
        String researchMode,
        BigDecimal sourceScore,
        String qualityStatus,
        Integer specificityScore,
        Integer confidenceScore,
        Integer duplicationScore,
        Integer routineEvidenceScore,
        Integer difficultyEvidenceScore,
        Integer sourceDiversityScore,
        Integer solutionLanguageRiskScore,
        String routineSummary,
        String painsSummary,
        String customerBehaviorSummary,
        String channelsSummary,
        String resultsSummary,
        String mechanismOpportunitiesSummary,
        String evidenceSummary,
        String sourceDomains,
        Instant qualityCheckedAt) {}

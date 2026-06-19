package com.marketinghub.nichocnae.evidencelevelgate;

/** Representa a entrada lida do backend para calcular o nível de evidência comercial E0-E5. */
public record EvidenceLevelGatePending(
        Long routineCardId,
        Long researchCycleId,
        String nicheName,
        String routineSummary,
        String painsSummary,
        String resultsSummary,
        String evidenceSummary,
        String sourceDomains,
        Integer confidenceScore,
        Integer routineEvidenceScore,
        Integer difficultyEvidenceScore,
        Integer sourceDiversityScore,
        Integer specificityScore,
        Integer qualityConfidenceScore) {}

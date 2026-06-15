package com.marketinghub.hypothesis.pain.service.pending;

import java.math.BigDecimal;

/** Perfil enriquecido do nicho usado para preservar sinais OPRM no pipeline de hipótese. */
public record HypothesisPainPendingEnrichmentProfile(
        Long id,
        Long researchCycleId,
        String cnaeCode,
        String cnaeDescription,
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
        String resultsSummary,
        String mechanismOpportunitiesSummary,
        String evidenceSummary,
        String sourceDomains,
        String personaSummary,
        String languagePatterns,
        String commercialTriggers,
        String objections
) {
}

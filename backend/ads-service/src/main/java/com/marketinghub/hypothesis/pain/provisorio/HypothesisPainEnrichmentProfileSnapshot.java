package com.marketinghub.hypothesis.pain.provisorio;

import java.math.BigDecimal;

/** Responsabilidade: transportar os sinais enriquecidos do nicho sem expor entidade externa ao service de hipótese. */
public record HypothesisPainEnrichmentProfileSnapshot(
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
        String personaDailyTasks,
        String painsSummary,
        String resultsSummary,
        String mechanismOpportunitiesSummary,
        String evidenceSummary,
        String sourceDomains,
        String personaSummary,
        String languagePatterns,
        String commercialTriggers,
        String objections) {}

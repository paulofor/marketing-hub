package com.marketinghub.nichocnae.meiaudiencesegmenter;

/** Rascunho validado da segmentação comportamental antes de persistir o perfil MEI/autônomo. */
public record MeiAudienceSegmentDraft(
        String audienceName,
        String occupationTerms,
        String workMode,
        String customerAcquisitionBehavior,
        String dailyRoutineSummary,
        String recurringTasksSummary,
        String operationalPainsSummary,
        String emotionalPainsSummary,
        String dreamsSummary,
        String fearsSummary,
        String languagePatterns,
        String channelsUsed,
        String recentSourceSummary,
        Integer autonomousProfessionalFitScore,
        Integer behavioralEvidenceScore,
        Integer sourceFreshnessScore,
        Integer outdatedSourceRiskScore,
        Integer structuredBusinessDriftRiskScore,
        Integer solutionLanguageRiskScore) {}

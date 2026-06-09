package com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.detailAudienceProfile;

import java.time.Instant;

/** DTO responsável por detalhar o perfil completo de público-alvo MEI/autônomo pesquisado pelo OPRM. */
public record MeiAudienceProfileDetailResponse(
    Long id,
    Long researchCycleId,
    Long routineCardId,
    Long sourceNicheCandidateId,
    Long marketNicheId,
    String cnaeCode,
    String cnaeDescription,
    String neutralNicheName,
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
    Integer solutionLanguageRiskScore,
    Instant createdAt,
    Instant updatedAt) {}

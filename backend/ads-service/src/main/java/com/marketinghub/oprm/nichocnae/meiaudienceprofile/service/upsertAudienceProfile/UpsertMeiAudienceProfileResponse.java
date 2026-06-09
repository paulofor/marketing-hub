package com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile;

import java.time.Instant;

/** DTO responsável por retornar a identificação do perfil de público-alvo MEI/autônomo gravado pelo OPRM. */
public record UpsertMeiAudienceProfileResponse(
    Long id,
    Long researchCycleId,
    Long routineCardId,
    Long sourceNicheCandidateId,
    Long marketNicheId,
    String cnaeCode,
    String audienceName,
    Integer autonomousProfessionalFitScore,
    Integer behavioralEvidenceScore,
    Integer sourceFreshnessScore,
    Integer solutionLanguageRiskScore,
    Instant createdAt,
    Instant updatedAt) {}

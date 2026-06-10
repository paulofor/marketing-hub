package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.time.Instant;

/** Resposta do backend após persistir o perfil segmentado MEI/autônomo. */
public record MeiAudienceSegmenterCompletionResponse(
        Long profileId,
        Long researchCycleId,
        Long routineCardId,
        String cycleStatus,
        String audienceName,
        Integer autonomousProfessionalFitScore,
        Integer behavioralEvidenceScore,
        Integer sourceFreshnessScore,
        Instant updatedAt) {}

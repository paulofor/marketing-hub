package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.time.Instant;

/** Saída persistida pelo backend após concluir a segmentação comportamental MEI/autônomo. */
public record MeiAudienceSegmenterOutput(
        Long profileId,
        Long researchCycleId,
        Long routineCardId,
        String cycleStatus,
        String audienceName,
        Integer autonomousProfessionalFitScore,
        Integer behavioralEvidenceScore,
        Integer sourceFreshnessScore,
        Instant updatedAt) {}

package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.completeStageExecution;

import java.time.Instant;

/** DTO responsável por retornar a conclusão da segmentação comportamental MEI/autônomo. */
public record CompleteMeiAudienceSegmenterResponse(
    Long profileId,
    Long researchCycleId,
    Long routineCardId,
    String cycleStatus,
    String audienceName,
    Integer autonomousProfessionalFitScore,
    Integer behavioralEvidenceScore,
    Integer sourceFreshnessScore,
    Instant updatedAt) {}

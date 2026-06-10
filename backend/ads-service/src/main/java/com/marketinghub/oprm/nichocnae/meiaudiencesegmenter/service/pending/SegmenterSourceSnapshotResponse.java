package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending;

import java.time.Instant;

/** DTO responsável por transportar fonte curta e indicadores de aderência para segmentação MEI/autônomo. */
public record SegmenterSourceSnapshotResponse(
    Long sourceSnapshotId,
    Long sourceCandidateId,
    String sourceUrl,
    String sourceDomain,
    String sourceTitle,
    String sourceType,
    String sourceClassificationType,
    Integer sourceFreshnessScore,
    Boolean outdatedSourceRisk,
    Integer brazilRelevanceScore,
    Integer autonomousProfessionalEvidenceScore,
    Boolean structuredBusinessDriftRisk,
    Boolean solutionLanguageRisk,
    Instant publishedAt,
    String snippet,
    String shortExcerpt) {}

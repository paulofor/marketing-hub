package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.time.Instant;

/** Fonte curta usada como evidência e indicador de atualidade na segmentação MEI/autônomo. */
public record SegmenterSourceSnapshot(
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

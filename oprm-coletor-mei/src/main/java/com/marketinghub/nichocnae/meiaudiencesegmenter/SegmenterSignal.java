package com.marketinghub.nichocnae.meiaudiencesegmenter;

/** Sinal extraído usado como evidência rastreável da segmentação MEI/autônomo. */
public record SegmenterSignal(
        Long extractedSignalId,
        Long sourceSnapshotId,
        Long sourceCandidateId,
        String signalType,
        String signalText,
        String evidenceExcerpt,
        String sourceDomain,
        Integer confidenceScore) {}

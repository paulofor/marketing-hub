package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending;

/** DTO responsável por transportar um sinal extraído como insumo da segmentação comportamental MEI/autônomo. */
public record SegmenterSignalResponse(
    Long extractedSignalId,
    Long sourceSnapshotId,
    Long sourceCandidateId,
    String signalType,
    String signalText,
    String evidenceExcerpt,
    String sourceDomain,
    Integer confidenceScore) {}

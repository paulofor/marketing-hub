package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.pending;

/** Sinal extraído usado como insumo contratual da síntese de rotina. */
public record SignalForRoutineSynthesis(
    Long extractedSignalId,
    Long sourceSnapshotId,
    Long sourceCandidateId,
    String signalType,
    String signalText,
    String evidenceExcerpt,
    String sourceDomain,
    Integer confidenceScore) {}

package com.marketinghub.nichocnae.routinesynthesizer;

/** Sinal estruturado recebido do backend como insumo da etapa seis. */
public record SignalForRoutineSynthesis(
        Long extractedSignalId,
        Long sourceSnapshotId,
        Long sourceCandidateId,
        String signalType,
        String signalText,
        String evidenceExcerpt,
        String sourceDomain,
        Integer confidenceScore) {}

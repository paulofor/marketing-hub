package com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution;

import java.time.Instant;

/** Representa um sinal estruturado persistido para uso nas etapas seguintes do pipeline OPRM NichoCNAE. */
public record ExtractedSignalResponse(
    Long extractedSignalId,
    Long researchCycleId,
    Long sourceSnapshotId,
    Long sourceCandidateId,
    String signalType,
    String signalText,
    String evidenceExcerpt,
    String sourceDomain,
    Integer confidenceScore,
    String createdBy,
    Instant createdAt) {}

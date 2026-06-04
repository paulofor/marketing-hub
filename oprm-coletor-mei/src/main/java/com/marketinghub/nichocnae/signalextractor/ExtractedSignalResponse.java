package com.marketinghub.nichocnae.signalextractor;

import java.time.Instant;

/** Representa um sinal persistido pelo backend após conclusão da etapa cinco. */
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

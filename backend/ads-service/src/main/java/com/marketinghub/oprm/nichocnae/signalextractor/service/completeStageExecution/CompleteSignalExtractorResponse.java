package com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution;

import java.util.List;

/** Representa o resultado da conclusão da etapa cinco para um snapshot curto. */
public record CompleteSignalExtractorResponse(
    Long sourceSnapshotId,
    Long researchCycleId,
    String signalExtractionStatus,
    Integer extractedSignalCount,
    Integer cycleTotalExtractedSignals,
    List<ExtractedSignalResponse> signals) {}

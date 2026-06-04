package com.marketinghub.nichocnae.signalextractor;

import java.util.List;

/** Representa a resposta do backend após persistir sinais da etapa cinco. */
public record SignalExtractorCompletionResponse(
        Long sourceSnapshotId,
        Long researchCycleId,
        String signalExtractionStatus,
        Integer extractedSignalCount,
        Integer cycleTotalExtractedSignals,
        List<ExtractedSignalResponse> signals) {}

package com.marketinghub.nichocnae.signalextractor;

import java.util.List;

/** Representa a saída operacional da etapa cinco concluída no backend. */
public record SignalExtractorOutput(
        Long sourceSnapshotId,
        Long researchCycleId,
        String signalExtractionStatus,
        Integer extractedSignalCount,
        Integer cycleTotalExtractedSignals,
        List<ExtractedSignalResponse> signals) {}

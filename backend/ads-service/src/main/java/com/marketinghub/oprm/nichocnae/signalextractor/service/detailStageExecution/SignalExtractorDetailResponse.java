package com.marketinghub.oprm.nichocnae.signalextractor.service.detailStageExecution;

import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.ExtractedSignalResponse;
import java.util.List;

/** Representa os sinais extraídos para acompanhamento da etapa cinco de um ciclo de pesquisa. */
public record SignalExtractorDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalSourceSnapshots,
    Integer cycleTotalExtractedSignals,
    List<ExtractedSignalResponse> signals) {}

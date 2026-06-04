package com.marketinghub.nichocnae.signalextractor;

import java.util.List;

/** Representa o payload enviado ao backend para concluir a etapa cinco de extração de sinais. */
public record SignalExtractorCompletionRequest(
        Long researchCycleId,
        Long sourceCandidateId,
        String sourceDomain,
        String extractionStatus,
        String createdBy,
        List<ExtractedSignal> signals) {}

package com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution;

import java.util.List;

/** Representa o payload de conclusão da etapa cinco com sinais estruturados extraídos de um snapshot. */
public record CompleteSignalExtractorRequest(
    Long researchCycleId,
    Long sourceCandidateId,
    String sourceDomain,
    String extractionStatus,
    String createdBy,
    List<SignalExtractionItemRequest> signals) {}

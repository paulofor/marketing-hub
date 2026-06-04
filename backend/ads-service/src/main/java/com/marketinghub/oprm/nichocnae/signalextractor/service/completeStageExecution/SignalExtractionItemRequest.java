package com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution;

/** Representa um sinal classificado extraído de um snapshot curto da etapa cinco. */
public record SignalExtractionItemRequest(
    String signalType, String signalText, String evidenceExcerpt, Integer confidenceScore) {}

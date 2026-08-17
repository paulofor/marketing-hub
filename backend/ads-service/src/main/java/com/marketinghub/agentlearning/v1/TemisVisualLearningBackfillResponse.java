package com.marketinghub.agentlearning.v1;

/** Responsabilidade: resumir a incorporação idempotente do histórico visual de um experimento. */
public record TemisVisualLearningBackfillResponse(
    Long experimentId,
    int scannedAssets,
    int scannedCreatives,
    int ingestedCases,
    long generatedRuns) {}

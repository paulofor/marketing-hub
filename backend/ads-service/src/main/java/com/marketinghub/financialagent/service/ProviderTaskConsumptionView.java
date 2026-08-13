package com.marketinghub.financialagent.service;

import java.time.Instant;

/**
 * Responsabilidade: expor dados financeiros imutáveis de uma task sem vazar a entidade persistida.
 */
public record ProviderTaskConsumptionView(
    Long salesVideoJobId,
    String providerTaskId,
    Integer sceneNumber,
    Integer plannedSceneCount,
    Integer durationSeconds,
    Integer estimatedCredits,
    Integer billedCredits,
    Instant acceptedAt) {}

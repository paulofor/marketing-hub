package com.marketinghub.product.service.valuechainposition;

/** Responsabilidade: indicar a passagem de aprendizado pendente sem reabrir seu histórico. */
public record ProductLearningCycleNavigationResponse(
    Long productId,
    Long cycleId,
    Long experimentId,
    Long chainDefinitionId,
    String stage,
    String status,
    String reason,
    String url) {}

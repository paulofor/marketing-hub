package com.marketinghub.productdiscovery.v1.service.resumePrivateValidationHandoff;

/** Resume o encaminhamento de um ciclo factual concluído para a validação privada PDE. */
public record ProductDiscoveryPrivateValidationHandoffResponse(
    Long cycleId,
    String sourceReference,
    int dossierReadyCount,
    String status,
    String nextActivity,
    String message) {}

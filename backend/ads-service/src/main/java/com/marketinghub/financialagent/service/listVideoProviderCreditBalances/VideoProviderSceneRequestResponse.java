package com.marketinghub.financialagent.service.listVideoProviderCreditBalances;

import java.time.Instant;

/** Responsabilidade: expor uma cena aceita e cobrável pelo provedor de vídeo. */
public record VideoProviderSceneRequestResponse(
    Long jobId,
    Long productionCycleId,
    int sceneNumber,
    int plannedSceneCount,
    String providerTaskId,
    Instant acceptedAt) {}

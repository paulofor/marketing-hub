package com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution;

import java.time.Instant;

/** Resposta de persistência da etapa E0-E5 com o novo estado gravado no ciclo e no cartão. */
public record CompleteEvidenceLevelGateResponse(
    Long routineCardId,
    Long researchCycleId,
    String cycleStatus,
    String evidenceLevel,
    String gateStatus,
    Boolean approvedForMaterialization,
    Integer confidenceScore,
    Instant checkedAt) {}

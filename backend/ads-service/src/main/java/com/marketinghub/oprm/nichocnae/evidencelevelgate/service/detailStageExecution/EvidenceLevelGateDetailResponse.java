package com.marketinghub.oprm.nichocnae.evidencelevelgate.service.detailStageExecution;

import java.time.Instant;

/** Detalhe persistido do gate comercial E0-E5 para relatório do usuário. */
public record EvidenceLevelGateDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Long routineCardId,
    String evidenceLevel,
    String gateStatus,
    Integer confidenceScore,
    String notes,
    String checkedBy,
    Instant checkedAt) {}

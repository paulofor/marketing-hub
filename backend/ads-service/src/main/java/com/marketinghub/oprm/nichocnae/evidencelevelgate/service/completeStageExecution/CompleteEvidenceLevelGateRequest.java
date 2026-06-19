package com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution;

/** Contrato usado pelo executor externo para persistir o resultado E0-E5 sem delegar regra de negócio ao backend. */
public record CompleteEvidenceLevelGateRequest(
    Long routineCardId,
    String evidenceLevel,
    String gateStatus,
    Boolean approvedForMaterialization,
    Integer confidenceScore,
    String rejectionReasons,
    String nextMovements,
    String checkedBy) {}

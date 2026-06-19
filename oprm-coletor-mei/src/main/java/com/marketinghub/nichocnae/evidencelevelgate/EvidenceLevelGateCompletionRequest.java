package com.marketinghub.nichocnae.evidencelevelgate;

/** Payload enviado ao backend para persistir o resultado calculado da etapa E0-E5. */
public record EvidenceLevelGateCompletionRequest(Long routineCardId, String evidenceLevel, String gateStatus, Boolean approvedForMaterialization, Integer confidenceScore, String rejectionReasons, String nextMovements, String checkedBy) {}

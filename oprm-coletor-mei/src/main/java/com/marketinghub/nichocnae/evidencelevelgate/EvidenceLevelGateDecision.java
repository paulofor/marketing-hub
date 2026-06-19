package com.marketinghub.nichocnae.evidencelevelgate;

/** Resultado calculado pelo executor para separar existência, dor, impacto econômico e intenção de compra. */
public record EvidenceLevelGateDecision(
        String evidenceLevel,
        String gateStatus,
        boolean approvedForMaterialization,
        int confidenceScore,
        String rejectionReasons,
        String nextMovements) {}

package com.marketinghub.worker.pipeline.hypothesisproof;

import java.util.List;

/** Responsabilidade: representar a saída estruturada da etapa Prova validada pelo schema da OpenAI. */
public record HypothesisProofOutput(
        String proofType,
        String proofAsset,
        String proofMessage,
        List<String> evidenceSignals,
        String collectionMethod,
        String credibilityRationale,
        String objectionReduced,
        String boundaryConditions,
        String summary
) {
    /** Normaliza listas opcionais para evitar nulos no payload final da etapa. */
    public HypothesisProofOutput {
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
    }
}

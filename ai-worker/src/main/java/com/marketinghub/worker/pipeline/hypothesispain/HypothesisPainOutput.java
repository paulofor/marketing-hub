package com.marketinghub.worker.pipeline.hypothesispain;

import java.util.List;

/** Responsabilidade: representar a saída estruturada da etapa Dor validada pelo schema da OpenAI. */
public record HypothesisPainOutput(
        String surface,
        String root,
        String emotional,
        String social,
        String cost,
        String summary,
        List<String> evidenceSignals
) {
    /** Normaliza listas opcionais para evitar nulos no payload final da etapa. */
    public HypothesisPainOutput {
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
    }
}

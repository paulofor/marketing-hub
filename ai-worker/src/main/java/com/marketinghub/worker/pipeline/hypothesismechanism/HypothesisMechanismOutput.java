package com.marketinghub.worker.pipeline.hypothesismechanism;

import java.util.List;

/** Responsabilidade: representar a saída estruturada da etapa Mecanismo validada pelo schema da OpenAI. */
public record HypothesisMechanismOutput(
        String mechanismName,
        String coreMechanism,
        String howItWorks,
        List<String> steps,
        String aiLeverage,
        String effortReduction,
        String whyBelievable,
        String boundaryConditions,
        String summary,
        List<String> evidenceSignals
) {
    /** Normaliza listas opcionais para evitar nulos no payload final da etapa. */
    public HypothesisMechanismOutput {
        steps = steps == null ? List.of() : List.copyOf(steps);
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
    }
}

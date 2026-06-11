package com.marketinghub.worker.pipeline.hypothesisresult;

import java.util.List;

/** Responsabilidade: representar a saída estruturada da etapa Resultado validada pelo schema da OpenAI. */
public record HypothesisResultOutput(
        String desiredOutcome,
        String measurableChange,
        String beforeAfterContrast,
        String fastWin,
        String businessValue,
        String plausibilityLimits,
        String summary,
        List<String> evidenceSignals
) {
    /** Normaliza listas opcionais para evitar nulos no payload final da etapa. */
    public HypothesisResultOutput {
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
    }
}

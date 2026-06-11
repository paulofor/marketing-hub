package com.marketinghub.worker.pipeline.hypothesisoffer;

import java.util.List;

/** Responsabilidade: representar a saída estruturada da etapa Oferta validada pelo schema da OpenAI. */
public record HypothesisOfferOutput(
        String offerName,
        String coreOffer,
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
    public HypothesisOfferOutput {
        steps = steps == null ? List.of() : List.copyOf(steps);
        evidenceSignals = evidenceSignals == null ? List.of() : List.copyOf(evidenceSignals);
    }
}

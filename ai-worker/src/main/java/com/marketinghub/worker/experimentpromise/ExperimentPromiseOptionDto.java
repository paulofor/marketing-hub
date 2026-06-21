package com.marketinghub.worker.experimentpromise;

/** Representa uma opção de contrato de promessa gerada para a tela de experimento. */
public record ExperimentPromiseOptionDto(
        String singlePain,
        String freeReward,
        String productOffer,
        String funnelPromise,
        String primaryCta,
        String reason) {
}

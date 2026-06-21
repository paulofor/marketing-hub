package com.marketinghub.experiment.service.generatepromise;

/** Responsabilidade: transportar uma opção de contrato de promessa única gerada por IA. */
public record ExperimentPromiseOptionDto(
        String singlePain,
        String freeReward,
        String productOffer,
        String funnelPromise,
        String primaryCta,
        String reason) {}

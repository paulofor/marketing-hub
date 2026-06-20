package com.marketinghub.experiment.service.generatepromise;

import java.util.UUID;

/** Responsabilidade: receber o contexto usado para sugerir contratos de promessa única do experimento. */
public record GenerateExperimentPromiseOptionsRequest(
        Long nicheId,
        UUID hypothesisId,
        String hypothesis,
        String currentSinglePain,
        String currentFreeReward,
        String currentFunnelPromise,
        String currentPrimaryCta) {}

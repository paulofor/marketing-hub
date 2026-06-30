package com.marketinghub.experiment.service.generatepromise;

import com.marketinghub.experiment.ExperimentType;
import java.util.UUID;

/** Responsabilidade: receber o contexto usado para sugerir contratos de promessa única do experimento. */
public record GenerateExperimentPromiseOptionsRequest(
        Long nicheId,
        UUID hypothesisId,
        ExperimentType experimentType,
        String hypothesis,
        String currentSinglePain,
        String currentFreeReward,
        String currentFunnelPromise,
        String currentPrimaryCta) {}

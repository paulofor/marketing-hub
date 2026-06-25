package com.marketinghub.experiment.run.service.create;

import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;

/**
 * Contrato recebido para criar uma nova execução operacional de experimento.
 */
public record CreateExperimentRunRequest(
        ExperimentRunMode mode,
        ExperimentRunStopPolicy stopPolicy,
        String createdBy
) {
}

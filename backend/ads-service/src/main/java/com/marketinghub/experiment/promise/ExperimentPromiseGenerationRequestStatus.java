package com.marketinghub.experiment.promise;

/** Responsabilidade: representar o estado operacional da solicitação de geração de promessa pelo AI Worker. */
public enum ExperimentPromiseGenerationRequestStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DISMISSED
}

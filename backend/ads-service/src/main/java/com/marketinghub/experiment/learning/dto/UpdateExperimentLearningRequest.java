package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Atualização de status enviada pelo worker.
 */
public record UpdateExperimentLearningRequest(
        @NotNull ExperimentLearningStatus status,
        ExperimentLearningPayloadDto payload,
        String failureReason) {
}

package com.marketinghub.experiment.learning.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload opcional para registrar quem solicitou a leitura automática.
 */
public record CreateExperimentLearningRequest(@Size(max = 191) String requestedBy) {
}

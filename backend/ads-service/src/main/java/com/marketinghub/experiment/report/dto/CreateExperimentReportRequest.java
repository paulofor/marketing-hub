package com.marketinghub.experiment.report.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload para solicitar a geração de um relatório de experimento.
 */
public record CreateExperimentReportRequest(
        @Size(max = 191) String requestedBy
) {
}

package com.marketinghub.experiment.report.dto;

import com.marketinghub.experiment.report.ExperimentReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload para atualizar o status de uma solicitação de relatório.
 */
public record UpdateExperimentReportRequest(
        @NotNull ExperimentReportStatus status,
        @Size(max = 512) String downloadUrl,
        String failureReason
) {
}

package com.marketinghub.experiment.report.dto;

import com.marketinghub.experiment.report.ExperimentReportStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Resumo de uma solicitação de relatório.
 */
@Data
public class ExperimentReportRequestDto {
    private Long id;
    private Long experimentId;
    private ExperimentReportStatus status;
    private Instant requestedAt;
    private Instant completedAt;
    private String requestedBy;
    private String downloadUrl;
    private String failureReason;
}

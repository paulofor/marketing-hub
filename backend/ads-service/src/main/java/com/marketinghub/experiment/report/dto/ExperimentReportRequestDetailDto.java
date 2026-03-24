package com.marketinghub.experiment.report.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO com informações completas da solicitação, incluindo o payload capturado.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExperimentReportRequestDetailDto extends ExperimentReportRequestDto {
    private String payloadSnapshot;
}

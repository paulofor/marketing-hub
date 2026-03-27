package com.marketinghub.experiment.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO completo usado pelo worker com os snapshots necessários para gerar o aprendizado.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExperimentLearningRequestDetailDto extends ExperimentLearningRequestDto {
    private String payloadSnapshot;
    private String resultPayload;
}

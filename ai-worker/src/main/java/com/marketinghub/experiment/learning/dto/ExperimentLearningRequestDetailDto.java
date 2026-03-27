package com.marketinghub.experiment.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO completo usado pelo worker com os snapshots necessários para gerar o aprendizado.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentLearningRequestDetailDto extends ExperimentLearningRequestDto {
    private String payloadSnapshot;
    private String resultPayload;
}

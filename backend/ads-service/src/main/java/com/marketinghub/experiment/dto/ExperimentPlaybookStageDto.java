package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.ExperimentStage;
import java.util.List;

/**
 * Stage metadata describing default metrics and recommended variables.
 */
public record ExperimentPlaybookStageDto(
        ExperimentStage stage,
        String title,
        String description,
        String defaultPrimaryMetric,
        List<String> guardrailMetrics,
        List<ExperimentPlaybookVariableDto> variables) {
}

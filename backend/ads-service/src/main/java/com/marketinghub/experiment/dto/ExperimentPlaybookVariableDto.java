package com.marketinghub.experiment.dto;

import java.util.List;

/**
 * Represents a recommended variable to test within a stage of the experiment.
 */
public record ExperimentPlaybookVariableDto(
        String id,
        String label,
        String description,
        List<String> aiOutputs,
        String suggestedPrimaryMetric) {
}

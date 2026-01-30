package com.marketinghub.experiment.dto;

import java.util.List;

/** Summary describing the current publishing status of an experiment. */
public record ExperimentDiagnosticsDto(
        ExperimentDiagnosticsSeverity severity,
        String headline,
        String description,
        String resolution,
        List<ExperimentPublishingArtifactDto> artifacts
) { }

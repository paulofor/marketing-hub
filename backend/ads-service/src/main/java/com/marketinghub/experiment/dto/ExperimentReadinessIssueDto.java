package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.TargetingElementType;

import java.util.List;

/**
 * Representa uma pendência pré-publicação detectada para um experimento.
 */
public record ExperimentReadinessIssueDto(
        ExperimentReadinessIssueType type,
        String title,
        String description,
        String recommendation,
        List<TargetingElementType> missingTargetingTypes
) { }

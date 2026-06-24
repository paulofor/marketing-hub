package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.TargetingElementType;

import java.util.List;

/**
 * Resumo das pendências básicas que impedem a publicação do experimento.
 */
public record ExperimentReadinessSummaryDto(
        boolean hasCreatives,
        long creativeCount,
        boolean hasLeadPortalFlow,
        long leadPortalFlowCount,
        boolean hasCompleteTargeting,
        boolean hasGeraLandingPipeline,
        long geraLandingCompletedStageCount,
        long geraLandingRequiredStageCount,
        List<TargetingElementType> missingTargetingTypes,
        List<ExperimentReadinessIssueDto> issues
) { }

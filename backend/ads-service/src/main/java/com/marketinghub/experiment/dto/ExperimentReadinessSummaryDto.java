package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.ExperimentCaptureDestinationType;
import com.marketinghub.targeting.TargetingElementType;

import java.util.List;

/**
 * Resumo das pendências básicas que impedem a publicação do experimento.
 */
public record ExperimentReadinessSummaryDto(
        boolean hasCreatives,
        long creativeCount,
        ExperimentCaptureDestinationType captureDestinationType,
        boolean hasLeadPortalFlow,
        long leadPortalFlowCount,
        boolean hasInstantForm,
        long instantFormCount,
        boolean hasCompleteTargeting,
        List<TargetingElementType> missingTargetingTypes,
        List<ExperimentReadinessIssueDto> issues
) { }

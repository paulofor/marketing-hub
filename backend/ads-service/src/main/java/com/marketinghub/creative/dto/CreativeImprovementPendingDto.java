package com.marketinghub.creative.dto;

import java.util.List;

/** Responsabilidade: transportar uma correção de anúncio decidida pelo agente ao AI Worker. */
public record CreativeImprovementPendingDto(
    Long creativeId,
    Long experimentId,
    Integer nextVersionNumber,
    String format,
    String revisedHeadline,
    String revisedPrimaryText,
    String revisedDescription,
    String revisedCta,
    String destinationUrl,
    String revisedImagePrompt,
    List<String> mandatoryVisualRequirements,
    List<String> forbiddenVisualElements,
    List<String> visualAcceptanceCriteria,
    List<String> referenceImageUrls,
    String agentReviewJson) {}

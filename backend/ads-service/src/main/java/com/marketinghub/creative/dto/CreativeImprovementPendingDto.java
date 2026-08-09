package com.marketinghub.creative.dto;

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
    String agentReviewJson) {}

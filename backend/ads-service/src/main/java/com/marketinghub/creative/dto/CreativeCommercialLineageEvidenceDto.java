package com.marketinghub.creative.dto;

import java.time.Instant;

/**
 * Responsabilidade: transportar a prova auditável de adoção comercial entre um experimento de
 * origem e seu sucessor.
 */
public record CreativeCommercialLineageEvidenceDto(
    String contractVersion,
    String verificationStatus,
    Long targetExperimentId,
    Long adoptedSourceExperimentId,
    Long sourceCreativeId,
    Long sourceCreativeExperimentId,
    boolean reusedCreative,
    boolean sameProduct,
    boolean sameHypothesis,
    boolean destinationUrlMatched,
    boolean checkoutUrlMatched,
    Boolean mediaUrlMatched,
    String sourceDestinationUrl,
    String targetDestinationUrl,
    String sourceCheckoutUrl,
    String targetCheckoutUrl,
    String sourceCreativeStatus,
    String sourceCreativeAgentReviewStatus,
    Instant sourceCreativeReviewedAt) {}

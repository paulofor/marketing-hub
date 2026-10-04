package com.marketinghub.creative.service.videoreview;

/** Expõe a decisão do backend sobre a ação de revisão disponível para uma peça específica. */
public record VideoReviewEligibility(
    VideoReviewState state,
    String reason,
    boolean approvalAvailable,
    boolean agentReviewRequestAvailable) {}

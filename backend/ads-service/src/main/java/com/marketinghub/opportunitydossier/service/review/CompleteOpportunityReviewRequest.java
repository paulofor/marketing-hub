package com.marketinghub.opportunitydossier.service.review;

import com.marketinghub.opportunitydossier.OpportunityReviewDecision;

/** Responsabilidade: receber o parecer funcional e sua auditoria bruta. */
public record CompleteOpportunityReviewRequest(
    OpportunityReviewDecision decision,
    String rationale,
    String risks,
    String recommendation,
    String rawModelResponse,
    String modelName) {}

package com.marketinghub.opportunitydossier.service.review;

import com.marketinghub.opportunitydossier.OpportunityReviewDecision;

/** Responsabilidade: receber o parecer independente de um agente. */
public record SubmitOpportunityReviewRequest(
    OpportunityReviewDecision decision, String rationale, String risks, String recommendation) {}

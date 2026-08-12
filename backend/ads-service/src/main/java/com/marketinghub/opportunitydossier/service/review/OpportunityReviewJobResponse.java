package com.marketinghub.opportunitydossier.service.review;

import java.util.List;

/** Responsabilidade: transportar o contexto congelado de um parecer ao agente executor. */
public record OpportunityReviewJobResponse(
    Long reviewId,
    Long dossierId,
    String agentKey,
    String authorityMode,
    String title,
    String targetAudience,
    String mainPain,
    String referenceProduct,
    String aiAdvantage,
    String proposedOffer,
    String deliveryModel,
    String knownRisks,
    String experimentRecommendation,
    List<Evidence> evidence) {
  /** Responsabilidade: transportar uma evidência verificável sem JSON aninhado em texto. */
  public record Evidence(String sourceUrl, String summary, String createdBy) {}
}

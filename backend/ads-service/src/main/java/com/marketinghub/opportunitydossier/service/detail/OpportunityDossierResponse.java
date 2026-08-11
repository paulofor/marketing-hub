package com.marketinghub.opportunitydossier.service.detail;

import com.marketinghub.opportunitydossier.OpportunityDossierStatus;
import com.marketinghub.opportunitydossier.OpportunityReviewDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar o dossiê completo como contrato imutável. */
public record OpportunityDossierResponse(
    Long id,
    String title,
    String ownerAgentKey,
    OpportunityDossierStatus status,
    String targetAudience,
    String mainPain,
    String referenceProduct,
    String aiAdvantage,
    String proposedOffer,
    BigDecimal preliminaryPrice,
    String deliveryModel,
    String knownRisks,
    String experimentRecommendation,
    String humanDecisionBy,
    Instant humanDecisionAt,
    Long convertedPlanId,
    Long productDiscoveryCycleId,
    Instant createdAt,
    Instant updatedAt,
    List<Evidence> evidence,
    List<Review> reviews) {
  /** Responsabilidade: apresentar uma evidência e sua origem. */
  public record Evidence(
      Long id, String sourceUrl, String summary, String createdBy, Instant createdAt) {}

  /** Responsabilidade: apresentar solicitação, decisão e conteúdo do parecer. */
  public record Review(
      Long id,
      String agentKey,
      OpportunityReviewDecision decision,
      String rationale,
      String risks,
      String recommendation,
      Instant requestedAt,
      Instant completedAt) {}
}

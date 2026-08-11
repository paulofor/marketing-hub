package com.marketinghub.opportunitydossier.service.create;

import java.math.BigDecimal;

/** Responsabilidade: receber os dados de uma nova oportunidade. */
public record CreateOpportunityDossierRequest(
    String title,
    String ownerAgentKey,
    String targetAudience,
    String mainPain,
    String referenceProduct,
    String aiAdvantage,
    String proposedOffer,
    BigDecimal preliminaryPrice,
    String deliveryModel,
    String knownRisks,
    String experimentRecommendation) {}

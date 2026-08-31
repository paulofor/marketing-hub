package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import java.math.BigDecimal;
import java.time.Instant;

/** Resposta de uma oportunidade PDE para análise comercial. */
public record ProductDiscoveryOpportunityResponse(
    Long id,
    Long cycleId,
    String name,
    String primaryAudience,
    String rootPain,
    String practicalPain,
    String emotionalPain,
    String scaleEvidence,
    String unmetnessEvidence,
    String pdeExperience,
    String firstCampaignAngle,
    String commercialRisk,
    String evidenceJson,
    BigDecimal score,
    ProductDiscoveryOpportunityMaturity maturity,
    ProductDiscoveryOpportunityDecision decision,
    Instant createdAt,
    Instant updatedAt) {}

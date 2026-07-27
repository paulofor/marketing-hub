package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Contrato de oportunidade retornada pelo worker de descoberta. */
public record ProductDiscoveryOpportunityResultRequest(
    @NotBlank String name,
    @NotBlank String primaryAudience,
    @NotBlank String rootPain,
    String practicalPain,
    String emotionalPain,
    String scaleEvidence,
    String unmetnessEvidence,
    String pdeExperience,
    String firstCampaignAngle,
    String commercialRisk,
    String evidenceJson,
    @NotNull BigDecimal score,
    @NotNull ProductDiscoveryOpportunityDecision decision) {}

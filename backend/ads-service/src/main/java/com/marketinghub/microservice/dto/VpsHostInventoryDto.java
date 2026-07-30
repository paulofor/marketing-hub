package com.marketinghub.microservice.dto;

import java.math.BigDecimal;

/** Projeção do cadastro operacional de características físicas e custos de um host VPS. */
public record VpsHostInventoryDto(
    String host,
    String providerName,
    String providerEvidence,
    String cpu,
    Integer memoryGb,
    Integer diskGb,
    String operatingSystem,
    BigDecimal monthlyCostBrl,
    String billingCycle,
    String costEvidence,
    String physicalSpecsEvidence,
    String notes) {}

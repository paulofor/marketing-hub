package com.marketinghub.microservice.dto;

import java.math.BigDecimal;

/** Dados editáveis do cadastro físico, financeiro e operacional de um host VPS. */
public record UpdateVpsHostInventoryRequest(
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

package com.marketinghub.pde.infrastructure.service.saveVps;

import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import java.math.BigDecimal;

/** Responsabilidade: receber os dados editáveis de uma VPS de PDE. */
public record SavePdeVpsServerRequest(
    String name,
    String provider,
    String ipAddress,
    String planName,
    String region,
    Integer vcpuCount,
    Integer ramGb,
    Integer storageGb,
    BigDecimal monthlyCostBrl,
    String productSlug,
    String environment,
    String domains,
    PdeVpsStatus status,
    String notes) {}

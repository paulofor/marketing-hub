package com.marketinghub.pde.infrastructure.service.listVps;

import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma VPS cadastrada para gestão administrativa dos PDEs. */
public record PdeVpsServerResponse(
    Long id,
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
    String notes,
    Instant createdAt,
    Instant updatedAt) {}

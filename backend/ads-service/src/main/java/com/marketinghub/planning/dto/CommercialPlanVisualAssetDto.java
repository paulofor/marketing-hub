package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import java.time.Instant;

/** Responsabilidade: expor um item governado do kit visual do plano comercial. */
public record CommercialPlanVisualAssetDto(
    Long id,
    String assetUrl,
    String label,
    String purpose,
    String origin,
    String rightsStatement,
    Integer versionNumber,
    CommercialPlanVisualAssetStatus status,
    Instant createdAt,
    Instant updatedAt) {}

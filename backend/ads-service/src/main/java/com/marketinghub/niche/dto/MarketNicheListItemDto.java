package com.marketinghub.niche.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: transportar uma linha da listagem administrativa de nichos. */
public record MarketNicheListItemDto(
        Long id,
        String name,
        Long enrichedNicheProfileId,
        Instant createdAt,
        BigDecimal totalCost,
        Long pipelineHypothesesCount,
        Long experimentsCount) {
}

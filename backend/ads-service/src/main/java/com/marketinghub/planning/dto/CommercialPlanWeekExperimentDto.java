package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor um experimento criado em uma semana do planejamento comercial. */
public record CommercialPlanWeekExperimentDto(
        Long id,
        String name,
        Long nicheId,
        String nicheName,
        String hypothesisId,
        String hypothesisTitle,
        String productType,
        String status,
        Instant createdAt,
        BigDecimal campaignCost,
        BigDecimal aiCost,
        BigDecimal videoCost,
        BigDecimal totalCost,
        BigDecimal revenue,
        Long clicks,
        Long leads,
        Integer checkoutClicks,
        Integer purchases,
        String result) {}

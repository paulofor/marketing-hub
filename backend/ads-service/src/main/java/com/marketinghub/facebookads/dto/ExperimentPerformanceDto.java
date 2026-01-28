package com.marketinghub.facebookads.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO returned to the frontend summarising the latest campaign snapshot.
 */
public record ExperimentPerformanceDto(
        Long experimentId,
        String experimentName,
        String campaignId,
        Instant capturedAt,
        LocalDate dateStart,
        LocalDate dateStop,
        BigDecimal spend,
        Long impressions,
        Long reach,
        Long clicks,
        Integer leads,
        BigDecimal ctr,
        BigDecimal cpc,
        BigDecimal cpm,
        BigDecimal cpl,
        String currency
) {}

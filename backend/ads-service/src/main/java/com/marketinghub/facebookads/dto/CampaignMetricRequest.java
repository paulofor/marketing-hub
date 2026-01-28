package com.marketinghub.facebookads.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body used by the worker to push campaign performance data.
 */
public record CampaignMetricRequest(
        @NotBlank String accountId,
        @NotNull Long experimentId,
        String dateStart,
        String dateStop,
        String currency,
        BigDecimal spend,
        Long impressions,
        Long reach,
        Long clicks,
        BigDecimal ctr,
        BigDecimal cpc,
        BigDecimal cpm,
        Integer leads,
        JsonNode rawInsights
) {}

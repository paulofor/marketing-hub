package com.marketinghub.marketresearch.dto;

import com.marketinghub.marketresearch.domain.MarketResearchStatus;

import java.time.Instant;
import java.util.List;

public record MarketResearchResponse(
        Long id,
        String query,
        String analysisObjective,
        MarketResearchStatus status,
        List<String> sources,
        String summary,
        String model,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}

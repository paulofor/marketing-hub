package com.marketinghub.marketresearch.service;

import com.marketinghub.marketresearch.domain.MarketResearchTask;
import com.marketinghub.marketresearch.dto.MarketResearchResponse;

public final class MarketResearchMapper {
    private MarketResearchMapper() {
    }

    public static MarketResearchResponse toResponse(MarketResearchTask task) {
        return new MarketResearchResponse(
                task.getId(),
                task.getQuery(),
                task.getAnalysisObjective(),
                task.getStatus(),
                task.getSources(),
                task.getSummary(),
                task.getModel(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}

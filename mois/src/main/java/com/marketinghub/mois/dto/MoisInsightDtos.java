package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;

public final class MoisInsightDtos {

    private MoisInsightDtos() {
    }

    public record InsightReportSummaryResponse(
            String reportId,
            String requestId,
            String nicheName,
            String marketTheme,
            String status,
            Instant createdAt
    ) {
    }

    public record InsightReportResponse(
            String reportId,
            String requestId,
            String nicheName,
            String marketTheme,
            String status,
            Instant createdAt,
            List<String> repeatedPromises,
            List<String> repeatedProofPatterns,
            List<String> pricingPatterns,
            List<String> funnelPatterns,
            List<String> gapOpportunities,
            List<String> recommendedNextActions
    ) {
    }

    public record InsightReportListResponse(List<InsightReportSummaryResponse> items) {
    }
}

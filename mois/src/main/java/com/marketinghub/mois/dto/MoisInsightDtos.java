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
            Instant createdAt,
            int offersAnalyzedCount
    ) {
    }

    public record InsightReportPatternResponse(
            String label,
            long count,
            double share
    ) {
    }

    public record SaturationSignalResponse(
            String category,
            String priceBand,
            long offerCount,
            double saturationScore
    ) {
    }

    public record GapOpportunityResponse(
            String gapType,
            String gapDescription,
            String whyItMatters,
            List<String> supportingOfferRefs,
            String priority,
            double confidence,
            List<String> scoringCriteria
    ) {
    }

    public record FrameworkRecommendationResponse(
            String dominantPain,
            String mostPromisedOutcome,
            String mostExploredMechanism,
            String mostUsedProof,
            List<String> subexploredOfferAngles
    ) {
    }

    public record InsightExecutiveSummaryResponse(
            String reportId,
            String requestId,
            String nicheName,
            String marketTheme,
            FrameworkRecommendationResponse frameworkRecommendation,
            List<GapOpportunityResponse> topGapOpportunities,
            List<SaturationSignalResponse> saturationSignals,
            List<String> decisionReadyActions
    ) {
    }

    public record InsightReportRequestSummary(
            String requestId,
            String nicheName,
            String marketTheme,
            String painOrOutcomeFocus,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record InsightReportResponse(
            String reportId,
            String requestId,
            String nicheName,
            String marketTheme,
            String status,
            Instant createdAt,
            InsightReportRequestSummary requestSummary,
            List<String> offersAnalyzed,
            List<InsightReportPatternResponse> repeatedPromises,
            List<InsightReportPatternResponse> repeatedProofPatterns,
            List<InsightReportPatternResponse> pricingPatterns,
            List<InsightReportPatternResponse> funnelPatterns,
            List<InsightReportPatternResponse> mechanismClaimPatterns,
            List<SaturationSignalResponse> saturationSignals,
            List<String> saturationNotes,
            List<GapOpportunityResponse> gapOpportunities,
            List<String> differentiationSignals,
            List<String> recommendedNextActions,
            FrameworkRecommendationResponse frameworkRecommendation
    ) {
    }

    public record InsightReportListResponse(List<InsightReportSummaryResponse> items) {
    }
}

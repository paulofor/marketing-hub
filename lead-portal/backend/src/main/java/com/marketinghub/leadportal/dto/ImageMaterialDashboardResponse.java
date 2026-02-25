package com.marketinghub.leadportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageMaterialDashboardResponse(
        String flowSlug,
        long totalSubmissions,
        long packagesQueued,
        long packagesInProgress,
        long packagesCompleted,
        long packagesFailed,
        long plannedImages,
        long imagesGenerated,
        BigDecimal estimatedCostUsd,
        List<CurrencyTotal> payments,
        List<PackageSummary> recentPackages) {

    public record CurrencyTotal(String currency, BigDecimal amount) {}

    public record PackageSummary(
            long packageId,
            UUID submissionId,
            String status,
            String professionalName,
            String contactSummary,
            String studioName,
            String location,
            List<String> services,
            Integer plannedOutputs,
            BigDecimal totalPrice,
            String currency,
            Instant createdAt,
            Instant updatedAt,
            String failureReason) {}
}

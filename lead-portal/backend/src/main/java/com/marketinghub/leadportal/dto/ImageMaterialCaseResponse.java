package com.marketinghub.leadportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ImageMaterialCaseResponse(
        UUID submissionId,
        String flowSlug,
        String activityType,
        String professionalName,
        String email,
        String contactSummary,
        String studioName,
        String location,
        List<String> services,
        Map<String, Object> answers,
        List<PackageDetails> packages) {

    public record PackageDetails(
            long packageId,
            String status,
            Integer plannedOutputs,
            Integer freeImages,
            String model,
            String prompt,
            BigDecimal totalPrice,
            String currency,
            String failureReason,
            Instant createdAt,
            Instant updatedAt,
            List<StatusHistory> history) {}

    public record StatusHistory(String status, Instant createdAt, String reason) {}
}

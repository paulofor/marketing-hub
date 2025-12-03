package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary information about image packages generated from Lead Portal submissions.
 */
public record LeadPortalImagePackageSummaryDto(
        Long id,
        UUID submissionId,
        String flowSlug,
        String name,
        String email,
        String phone,
        FlowSubmissionImagePackageStatus status,
        String prompt,
        String model,
        Integer plannedOutputs,
        Integer freeImages,
        Integer generatedImageCount,
        Integer watermarkedImageCount,
        Instant createdAt,
        Instant updatedAt,
        String failureReason
) {}

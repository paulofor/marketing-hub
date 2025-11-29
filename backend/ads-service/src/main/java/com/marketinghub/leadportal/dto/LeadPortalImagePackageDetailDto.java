package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed information about a Lead Portal image package including related media assets.
 */
public record LeadPortalImagePackageDetailDto(
        Long id,
        UUID submissionId,
        FlowSubmissionImagePackageStatus status,
        String prompt,
        String model,
        Integer plannedOutputs,
        Integer freeImages,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        SubmissionInfo submission,
        ImageReference originalImage,
        List<ImageReference> generatedImages
) {
    public record SubmissionInfo(
            String flowSlug,
            String name,
            String email,
            String phone,
            String imageQuestionKey
    ) {}

    public record ImageReference(
            String type,
            String url,
            String downloadUrl,
            String accessType,
            Long assetId,
            Integer position,
            String prompt,
            String model,
            Instant createdAt,
            Long itemId,
            String storedFileName
    ) {}
}

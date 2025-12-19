package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageLifecycleStatus;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Detailed information about a Lead Portal image package including related media assets.
 */
public record LeadPortalImagePackageDetailDto(
        Long id,
        UUID submissionId,
        FlowSubmissionImagePackageStatus status,
        FlowSubmissionImagePackageLifecycleStatus lifecycleStatus,
        Boolean sendImagesAsZip,
        String prompt,
        String model,
        Integer plannedOutputs,
        Integer freeImages,
        Integer watermarkedImageCount,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<StatusHistoryEntry> history,
        ZipExport sampleZip,
        SubmissionInfo submission,
        ImageReference originalImage,
        List<ImageReference> generatedImages,
        Long imageModelId,
        String imageModelName,
        Long imageModelQualityId,
        String imageModelQualityName,
        String imageOrientation,
        Integer imageWidth,
        Integer imageHeight,
        BigDecimal imageUnitPriceUsd,
        BigDecimal imageTotalPriceUsd,
        String imageCurrency
) {
    public record SubmissionInfo(
            String flowSlug,
            String name,
            String email,
            String phone,
            String imageQuestionKey
    ) {}

    public record StatusHistoryEntry(
            FlowSubmissionImagePackageStatus status,
            String failureReason,
            Instant occurredAt
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
            String storedFileName,
            WatermarkReference watermark
    ) {}

    public record ZipExport(
            String objectKey,
            String downloadUrl,
            Instant generatedAt
    ) {}

    public record WatermarkReference(
            Long assetId,
            String url,
            String downloadUrl,
            Instant createdAt,
            String storedFileName
    ) {}
}

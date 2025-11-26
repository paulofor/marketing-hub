package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.entity.FlowSubmissionEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record FlowSubmissionImagePackageResponse(
        Long id,
        UUID submissionId,
        String flowSlug,
        String name,
        String email,
        String status,
        Integer plannedOutputs,
        Integer freeImages,
        String model,
        String prompt,
        Instant createdAt) {

    public static FlowSubmissionImagePackageResponse from(
            FlowSubmissionImagePackageEntity imagePackage, FlowSubmissionEntity submission) {
        return new FlowSubmissionImagePackageResponse(
                imagePackage.getId(),
                imagePackage.getSubmissionId(),
                Optional.ofNullable(submission).map(FlowSubmissionEntity::getFlowSlug).orElse(null),
                Optional.ofNullable(submission).map(FlowSubmissionEntity::getName).orElse(null),
                Optional.ofNullable(submission).map(FlowSubmissionEntity::getEmail).orElse(null),
                imagePackage.getStatus(),
                imagePackage.getPlannedOutputs(),
                imagePackage.getFreeImages(),
                imagePackage.getModel(),
                imagePackage.getPrompt(),
                imagePackage.getCreatedAt());
    }
}

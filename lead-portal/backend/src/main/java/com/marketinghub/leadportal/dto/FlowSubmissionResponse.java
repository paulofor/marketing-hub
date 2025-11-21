package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.FlowSubmission;
import java.time.Instant;
import java.util.UUID;

public record FlowSubmissionResponse(
        UUID id,
        String flowSlug,
        String name,
        String email,
        String imageUrl,
        Instant createdAt) {

    public static FlowSubmissionResponse from(FlowSubmission submission, String imageUrl) {
        return new FlowSubmissionResponse(
                submission.id(), submission.flowSlug(), submission.name(), submission.email(), imageUrl, submission.createdAt());
    }
}

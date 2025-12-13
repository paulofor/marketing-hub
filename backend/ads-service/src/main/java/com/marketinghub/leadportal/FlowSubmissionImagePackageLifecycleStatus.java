package com.marketinghub.leadportal;

/**
 * Expanded status used to represent post-processing stages for Lead Portal image packages.
 */
public enum FlowSubmissionImagePackageLifecycleStatus {
    RECEIVED,
    RECENT,
    PROCESSING,
    WATERMARK_PENDING,
    WATERMARKING,
    COMPLETED,
    FAILED,
    ZIP_GENERATING,
    SAMPLE_EMAIL_SENDING,
    SAMPLE_EMAIL_SENT;

    public static FlowSubmissionImagePackageLifecycleStatus fromBaseStatus(FlowSubmissionImagePackageStatus status) {
        if (status == null) {
            return RECEIVED;
        }
        return switch (status) {
            case RECEIVED -> RECEIVED;
            case RECENT -> RECENT;
            case PROCESSING -> PROCESSING;
            case WATERMARK_PENDING -> WATERMARK_PENDING;
            case WATERMARKING -> WATERMARKING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
        };
    }
}

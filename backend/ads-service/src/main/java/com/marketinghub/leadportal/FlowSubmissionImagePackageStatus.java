package com.marketinghub.leadportal;

/**
 * Possible states for image packages generated from lead portal flow submissions.
 */
public enum FlowSubmissionImagePackageStatus {
    RECEIVED,
    RECENT,
    PROCESSING,
    WATERMARK_PENDING,
    WATERMARKING,
    COMPLETED,
    SAMPLE_EMAIL_OPENED,
    SAMPLE_IMAGES_VIEWED,
    FAILED
}

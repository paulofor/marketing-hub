package com.marketinghub.emailservice.service.client;

/**
 * Status possíveis para os pacotes retornados pelo backend do Marketing Hub.
 */
public enum FlowSubmissionImagePackageStatus {
    RECEIVED,
    RECENT,
    PROCESSING,
    WATERMARK_PENDING,
    WATERMARKING,
    COMPLETED,
    FAILED
}

package com.marketinghub.emailservice.leadportal.service;

import com.marketinghub.emailservice.service.client.FlowSubmissionImagePackageStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload com os dados necessários para enviar os pacotes de imagens do Lead Portal.
 */
public record LeadPortalImagePackageExportItem(
        long packageId,
        String submissionId,
        String submissionName,
        String submissionEmail,
        FlowSubmissionImagePackageStatus status,
        long experimentId,
        String experimentName,
        String sampleSubject,
        String samplePreview,
        String sampleBody,
        String sampleCallToAction,
        String sampleModel,
        String samplePrompt,
        Instant sampleUpdatedAt,
        int notificationAttempts,
        Instant notificationLastAttempt,
        byte[] zipBytes,
        String zipObjectKey,
        String attachmentName,
        int imageCount,
        String emailSubject,
        String emailPlainBody,
        String emailHtmlBody,
        PaymentInfo paymentInfo
) {

    public record PaymentInfo(
            Long purchaseId,
            String checkoutUrl,
            BigDecimal amount,
            String currency,
            Instant expiresAt,
            String statementDescriptor
    ) {
    }
}

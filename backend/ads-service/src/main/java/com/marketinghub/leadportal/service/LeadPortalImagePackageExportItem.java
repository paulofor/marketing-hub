package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload enviado para serviços externos consumirem pacotes de imagens concluídos.
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

package com.marketinghub.emailservice.service.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa a resposta do endpoint de exportação de pacotes de imagem.
 */
public record LeadPortalImagePackageExportResponse(
        long packageId,
        UUID submissionId,
        String submissionName,
        String submissionEmail,
        FlowSubmissionImagePackageStatus status,
        long experimentId,
        String experimentName,
        SampleEmail sampleEmail,
        EmailContent emailContent,
        Attachment attachment,
        PaymentInfo paymentInfo,
        int notificationAttempts,
        Instant notificationLastAttempt
) {

    public record SampleEmail(
            String subject,
            String preview,
            String body,
            String callToAction,
            String model,
            String prompt,
            Instant updatedAt
    ) {
    }

    public record EmailContent(
            String subject,
            String plainBody,
            String htmlBody
    ) {
    }

    public record Attachment(
            String fileName,
            String base64Content,
            int imageCount,
            long sizeBytes,
            String storedFileName,
            String downloadUrl
    ) {
    }

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

package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa o payload enviado ao serviço de e-mail com dados do pacote e conteúdo do e-mail de amostra.
 */
public record LeadPortalImagePackageExportDto(
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

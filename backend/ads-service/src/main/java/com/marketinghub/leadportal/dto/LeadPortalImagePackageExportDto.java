package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.List;
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
        boolean sendImagesAsZip,
        int imageCount,
        Attachment attachment,
        List<Attachment> attachments,
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
            String contentType,
            String base64Content,
            Integer imageCount,
            long sizeBytes,
            String storedFileName,
            String downloadUrl
    ) {
    }
}

package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.List;

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
        boolean sendImagesAsZip,
        int imageCount,
        List<Attachment> attachments,
        String emailSubject,
        String emailPlainBody,
        String emailHtmlBody
) {

    public record Attachment(
            String fileName,
            String contentType,
            byte[] bytes,
            String storedFileName,
            String downloadUrl,
            Integer imageCount
    ) {
    }
}

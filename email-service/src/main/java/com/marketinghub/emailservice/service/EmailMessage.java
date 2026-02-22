package com.marketinghub.emailservice.service;

import java.util.List;

public record EmailMessage(
        String from,
        String fromName,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String htmlBody,
        String textBody,
        List<EmailAttachmentResource> attachments
) {
}

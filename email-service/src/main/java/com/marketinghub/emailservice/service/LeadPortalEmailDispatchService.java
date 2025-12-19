package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.config.LeadPortalDispatchProperties;
import com.marketinghub.emailservice.service.client.LeadPortalImagePackageClient;
import com.marketinghub.emailservice.service.client.LeadPortalImagePackageExportResponse;
import com.marketinghub.emailservice.service.client.RemoteAsset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalEmailDispatchService.class);
    private static final MediaType ZIP_MEDIA_TYPE = MediaType.parseMediaType("application/zip");
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_OCTET_STREAM;

    private final LeadPortalImagePackageClient leadPortalImagePackageClient;
    private final EmailSenderService emailSenderService;
    private final EmailServiceProperties emailServiceProperties;
    private final LeadPortalDispatchProperties dispatchProperties;

    public LeadPortalEmailDispatchService(LeadPortalImagePackageClient leadPortalImagePackageClient,
                                          EmailSenderService emailSenderService,
                                          EmailServiceProperties emailServiceProperties,
                                          LeadPortalDispatchProperties dispatchProperties) {
        this.leadPortalImagePackageClient = leadPortalImagePackageClient;
        this.emailSenderService = emailSenderService;
        this.emailServiceProperties = emailServiceProperties;
        this.dispatchProperties = dispatchProperties;
    }

    @Scheduled(initialDelayString = "${lead-portal.dispatch.initial-delay:20000}",
            fixedDelayString = "${lead-portal.dispatch.poll-interval:60000}")
    public void pollAndDispatch() {
        if (!dispatchProperties.enabled()) {
            return;
        }
        List<LeadPortalImagePackageExportResponse> packages = leadPortalImagePackageClient.fetchPackages(dispatchProperties.batchSize());
        if (packages.isEmpty()) {
            return;
        }
        for (LeadPortalImagePackageExportResponse item : packages) {
            try {
                sendEmail(item);
                leadPortalImagePackageClient.acknowledge(item.packageId(), true, null);
                log.info("Pacote {} enviado para {}", item.packageId(), item.submissionEmail());
            } catch (Exception ex) {
                log.error("Falha ao enviar pacote {} para {}", item.packageId(), item.submissionEmail(), ex);
                try {
                    leadPortalImagePackageClient.acknowledge(item.packageId(), false, resolveRootCauseMessage(ex));
                } catch (Exception ackEx) {
                    log.error("Falha ao registrar a falha do pacote {}", item.packageId(), ackEx);
                }
            }
        }
    }

    private void sendEmail(LeadPortalImagePackageExportResponse item) {
        if (!StringUtils.hasText(item.submissionEmail())) {
            throw new IllegalArgumentException("Destinatário vazio para o pacote " + item.packageId());
        }
        LeadPortalImagePackageExportResponse.EmailContent emailContent = item.emailContent();
        if (emailContent == null) {
            throw new IllegalStateException("Conteúdo de e-mail ausente para o pacote " + item.packageId());
        }
        List<LeadPortalImagePackageExportResponse.Attachment> attachmentDtos = resolveAttachments(item);
        if (attachmentDtos.isEmpty()) {
            throw new IllegalStateException("Nenhum anexo encontrado para o pacote " + item.packageId());
        }

        List<EmailAttachmentResource> attachments = new ArrayList<>();
        for (LeadPortalImagePackageExportResponse.Attachment attachmentDto : attachmentDtos) {
            byte[] attachmentBytes = decodeAttachment(attachmentDto);
            if (attachmentBytes.length == 0) {
                throw new IllegalStateException("Anexo vazio para o pacote " + item.packageId());
            }
            String attachmentName = resolveAttachmentName(attachmentDto, item);
            MediaType mediaType = resolveMediaType(attachmentDto, item);
            RemoteAsset asset = new RemoteAsset(attachmentName, mediaType, attachmentBytes);
            attachments.add(new EmailAttachmentResource(asset, false, null));
        }

        EmailMessage message = new EmailMessage(
                emailServiceProperties.defaultFromAddress(),
                List.of(item.submissionEmail()),
                List.of(),
                List.of(),
                emailContent.subject(),
                emailContent.htmlBody(),
                emailContent.plainBody(),
                attachments
        );

        log.info("Enviando pacote {} para {} com assunto '{}' (anexos={})",
                item.packageId(),
                item.submissionEmail(),
                emailContent.subject(),
                attachments.size());
        emailSenderService.send(message);
    }

    private List<LeadPortalImagePackageExportResponse.Attachment> resolveAttachments(LeadPortalImagePackageExportResponse item) {
        if (item.attachments() != null && !item.attachments().isEmpty()) {
            return item.attachments();
        }
        if (item.attachment() != null) {
            return List.of(item.attachment());
        }
        return List.of();
    }

    private MediaType resolveMediaType(LeadPortalImagePackageExportResponse.Attachment attachment,
                                       LeadPortalImagePackageExportResponse item) {
        if (attachment != null && StringUtils.hasText(attachment.contentType())) {
            try {
                return MediaType.parseMediaType(attachment.contentType());
            } catch (Exception ignored) {
                // fallback abaixo
            }
        }
        return item.sendImagesAsZip() ? ZIP_MEDIA_TYPE : DEFAULT_MEDIA_TYPE;
    }

    private String resolveAttachmentName(LeadPortalImagePackageExportResponse.Attachment attachment,
                                         LeadPortalImagePackageExportResponse item) {
        if (attachment != null && StringUtils.hasText(attachment.fileName())) {
            return attachment.fileName();
        }
        return item.sendImagesAsZip()
                ? "imagens-watermark-" + item.packageId() + ".zip"
                : "imagens-watermark-" + item.packageId() + "-imagem.jpg";
    }

    private byte[] decodeAttachment(LeadPortalImagePackageExportResponse.Attachment attachment) {
        if (attachment == null || !StringUtils.hasText(attachment.base64Content())) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(attachment.base64Content());
    }

    private String resolveRootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return StringUtils.hasText(cursor.getMessage()) ? cursor.getMessage() : throwable.getMessage();
    }
}

package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.config.LeadPortalDispatchProperties;
import com.marketinghub.emailservice.config.LeadPortalPaymentLinkProperties;
import com.marketinghub.emailservice.model.EmailLog;
import com.marketinghub.emailservice.service.client.LeadPortalImagePackageClient;
import com.marketinghub.emailservice.service.client.LeadPortalImagePackageExportResponse;
import com.marketinghub.emailservice.service.client.RemoteAsset;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LeadPortalEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalEmailDispatchService.class);
    private static final MediaType ZIP_MEDIA_TYPE = MediaType.parseMediaType("application/zip");

    private final LeadPortalImagePackageClient leadPortalImagePackageClient;
    private final EmailSenderService emailSenderService;
    private final EmailServiceProperties emailServiceProperties;
    private final LeadPortalDispatchProperties dispatchProperties;
    private final EmailLogService emailLogService;
    private final TrackingPixelService trackingPixelService;
    private final LeadPortalPaymentLinkProperties paymentLinkProperties;

    public LeadPortalEmailDispatchService(LeadPortalImagePackageClient leadPortalImagePackageClient,
                                          EmailSenderService emailSenderService,
                                          EmailServiceProperties emailServiceProperties,
                                          LeadPortalDispatchProperties dispatchProperties,
                                          EmailLogService emailLogService,
                                          TrackingPixelService trackingPixelService,
                                          LeadPortalPaymentLinkProperties paymentLinkProperties) {
        this.leadPortalImagePackageClient = leadPortalImagePackageClient;
        this.emailSenderService = emailSenderService;
        this.emailServiceProperties = emailServiceProperties;
        this.dispatchProperties = dispatchProperties;
        this.emailLogService = emailLogService;
        this.trackingPixelService = trackingPixelService;
        this.paymentLinkProperties = paymentLinkProperties;
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
        byte[] attachmentBytes = decodeAttachment(item.attachment());
        if (attachmentBytes.length == 0) {
            throw new IllegalStateException("Arquivo compactado vazio para o pacote " + item.packageId());
        }
        String attachmentName = item.attachment() != null && StringUtils.hasText(item.attachment().fileName())
                ? item.attachment().fileName()
                : "imagens-watermark-" + item.packageId() + ".zip";

        EmailLog emailLog = emailLogService.createPendingLog(
                item.submissionEmail(),
                emailContent.subject(),
                "lead-portal-package-" + item.packageId());

        PaymentBodies paymentBodies = enrichWithPaymentLink(emailContent, item.paymentInfo(), item.packageId());
        String trackingPixelUrl = trackingPixelService.buildTrackingPixelUrl(emailLog.getRequestId());
        String htmlBody = trackingPixelService.appendTrackingPixel(paymentBodies.htmlBody(), trackingPixelUrl);

        RemoteAsset asset = new RemoteAsset(attachmentName, ZIP_MEDIA_TYPE, attachmentBytes);
        EmailAttachmentResource attachment = new EmailAttachmentResource(asset, false, null);

        EmailMessage message = new EmailMessage(
                emailServiceProperties.defaultFromAddress(),
                List.of(item.submissionEmail()),
                List.of(),
                List.of(),
                emailContent.subject(),
                htmlBody,
                paymentBodies.plainBody(),
                List.of(attachment)
        );

        Long purchaseId = item.paymentInfo() != null ? item.paymentInfo().purchaseId() : null;
        log.info("Enviando pacote {} para {} com assunto '{}' (arquivo='{}', tamanho={} bytes, purchaseId={})",
                item.packageId(),
                item.submissionEmail(),
                emailContent.subject(),
                attachmentName,
                attachmentBytes.length,
                purchaseId);

        try {
            emailSenderService.send(message);
            emailLogService.markSent(emailLog.getRequestId());
        } catch (Exception ex) {
            emailLogService.markFailed(emailLog.getRequestId(), ex.getMessage());
            throw ex;
        }
    }

    private PaymentBodies enrichWithPaymentLink(LeadPortalImagePackageExportResponse.EmailContent emailContent,
                                                LeadPortalImagePackageExportResponse.PaymentInfo paymentInfo,
                                                long packageId) {
        String html = emailContent.htmlBody() != null ? emailContent.htmlBody() : "";
        String plain = emailContent.plainBody() != null ? emailContent.plainBody() : "";
        if (paymentInfo == null || !StringUtils.hasText(paymentInfo.checkoutUrl())) {
            return new PaymentBodies(html, plain);
        }
        String paymentUrl = resolvePaymentUrl(packageId, paymentInfo);
        validatePaymentUrl(paymentUrl);
        String normalizedHtml = html.contains(paymentUrl) ? html : html + buildHtmlPaymentBlock(paymentInfo, paymentUrl);
        String normalizedPlain = plain.contains(paymentUrl) ? plain : buildPlainPaymentBlock(plain, paymentInfo, paymentUrl);
        return new PaymentBodies(normalizedHtml, normalizedPlain);
    }

    private String resolvePaymentUrl(long packageId, LeadPortalImagePackageExportResponse.PaymentInfo paymentInfo) {
        String directUrl = paymentInfo.checkoutUrl();
        String entrypoint = paymentLinkProperties.getEntrypointBaseUrl();
        if (!StringUtils.hasText(entrypoint)) {
            return directUrl;
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(entrypoint.trim());
            builder.replaceQueryParam(paymentLinkProperties.getPackageIdQueryParam(), packageId);
            if (paymentInfo.purchaseId() != null && StringUtils.hasText(paymentLinkProperties.getPurchaseIdQueryParam())) {
                builder.replaceQueryParam(paymentLinkProperties.getPurchaseIdQueryParam(), paymentInfo.purchaseId());
            }
            if (paymentInfo.expiresAt() != null) {
                builder.replaceQueryParam("expiresAt", paymentInfo.expiresAt());
            }
            return builder.build(true).toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("URL base do portal de pagamentos inválida ({}). Usando link direto do Mercado Pago.", entrypoint, ex);
            return directUrl;
        }
    }

    private void validatePaymentUrl(String checkoutUrl) {
        if (!paymentLinkProperties.isValidateHost()) {
            return;
        }
        try {
            URI uri = new URI(checkoutUrl.trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("Link de pagamento sem host definido");
            }
            boolean allowed = paymentLinkProperties.getAllowedHosts().stream()
                    .anyMatch(allowedHost -> host.equalsIgnoreCase(allowedHost));
            if (!allowed) {
                throw new IllegalArgumentException("Host do link de pagamento não autorizado: " + host);
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL de pagamento inválida", ex);
        }
    }

    private String buildHtmlPaymentBlock(LeadPortalImagePackageExportResponse.PaymentInfo paymentInfo, String paymentUrl) {
        String descriptor = StringUtils.hasText(paymentInfo.statementDescriptor())
                ? paymentInfo.statementDescriptor()
                : "Mercado Pago";
        String amount = formatAmount(paymentInfo.amount(), paymentInfo.currency());
        String label = paymentLinkProperties.getButtonText();
        if (StringUtils.hasText(amount)) {
            label = label + " — " + amount;
        }
        String buttonColor = StringUtils.hasText(paymentLinkProperties.getButtonColor())
                ? paymentLinkProperties.getButtonColor()
                : "#00a650";
        return new StringBuilder()
                .append("<div style=\"margin:24px 0;padding:16px 20px;border:1px solid #e3e3e3;border-radius:8px;background:#f8f8f8;\">")
                .append("<p><strong>Faça o pagamento e libere as imagens originais.</strong></p>")
                .append("<p>Processado por ")
                .append(HtmlUtils.htmlEscape(descriptor))
                .append("</p>")
                .append("<p style=\"text-align:center;\"><a href=\"")
                .append(HtmlUtils.htmlEscape(paymentUrl.trim()))
                .append("\" target=\"_blank\" rel=\"noopener\" style=\"display:inline-block;padding:14px 28px;font-weight:600;border-radius:6px;text-decoration:none;color:#fff;background:")
                .append(HtmlUtils.htmlEscape(buttonColor))
                .append(";\">")
                .append(HtmlUtils.htmlEscape(label))
                .append("</a></p>")
                .append("</div>")
                .toString();
    }
    private String buildPlainPaymentBlock(String plainBody, LeadPortalImagePackageExportResponse.PaymentInfo paymentInfo, String paymentUrl) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(plainBody)) {
            builder.append(plainBody.trim()).append("\n\n");
        }
        builder.append(paymentLinkProperties.getPlainTextIntro()).append("\n")
                .append(paymentUrl);
        String amount = formatAmount(paymentInfo.amount(), paymentInfo.currency());
        if (StringUtils.hasText(amount)) {
            builder.append("\nValor: ").append(amount);
        }
        String descriptor = StringUtils.hasText(paymentInfo.statementDescriptor())
                ? paymentInfo.statementDescriptor()
                : "Mercado Pago";
        builder.append("\nPagamento processado por ").append(descriptor).append("\n");
        return builder.toString();
    }
    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        Locale locale = "BRL".equalsIgnoreCase(currency) ? new Locale("pt", "BR") : Locale.US;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        try {
            return formatter.format(amount);
        } catch (IllegalArgumentException ignored) {
            return amount.toPlainString();
        }
    }

    private record PaymentBodies(String htmlBody, String plainBody) {
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

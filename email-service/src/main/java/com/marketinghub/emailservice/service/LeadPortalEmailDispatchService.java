package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.LeadPortalDispatchProperties;
import com.marketinghub.emailservice.config.LeadPortalPaymentLinkProperties;
import com.marketinghub.emailservice.leadportal.service.LeadPortalImagePackageExportItem;
import com.marketinghub.emailservice.leadportal.service.LeadPortalPackageNotificationService;
import com.marketinghub.emailservice.leadportal.service.LeadPortalTrackingLinkService;
import com.marketinghub.emailservice.model.EmailLog;
import com.marketinghub.emailservice.settings.EmailSmtpConfigurationService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LeadPortalEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalEmailDispatchService.class);

    private final LeadPortalPackageNotificationService leadPortalPackageNotificationService;
    private final EmailSenderService emailSenderService;
    private final LeadPortalDispatchProperties dispatchProperties;
    private final EmailLogService emailLogService;
    private final TrackingPixelService trackingPixelService;
    private final LeadPortalPaymentLinkProperties paymentLinkProperties;
    private final LeadPortalTrackingLinkService trackingLinkService;
    private final EmailSmtpConfigurationService smtpConfigurationService;

    public LeadPortalEmailDispatchService(LeadPortalPackageNotificationService leadPortalPackageNotificationService,
                                          EmailSenderService emailSenderService,
                                          LeadPortalDispatchProperties dispatchProperties,
                                          EmailLogService emailLogService,
                                          TrackingPixelService trackingPixelService,
                                          LeadPortalPaymentLinkProperties paymentLinkProperties,
                                          LeadPortalTrackingLinkService trackingLinkService,
                                          EmailSmtpConfigurationService smtpConfigurationService) {
        this.leadPortalPackageNotificationService = leadPortalPackageNotificationService;
        this.emailSenderService = emailSenderService;
        this.dispatchProperties = dispatchProperties;
        this.emailLogService = emailLogService;
        this.trackingPixelService = trackingPixelService;
        this.paymentLinkProperties = paymentLinkProperties;
        this.trackingLinkService = trackingLinkService;
        this.smtpConfigurationService = smtpConfigurationService;
    }

    @Scheduled(initialDelayString = "${lead-portal.dispatch.initial-delay:20000}",
            fixedDelayString = "${lead-portal.dispatch.poll-interval:60000}")
    public void pollAndDispatch() {
        if (!dispatchProperties.enabled()) {
            return;
        }
        List<LeadPortalImagePackageExportItem> packages =
                leadPortalPackageNotificationService.exportReadyPackages(dispatchProperties.batchSize());
        if (packages.isEmpty()) {
            return;
        }
        for (LeadPortalImagePackageExportItem item : packages) {
            try {
                sendEmail(item);
                leadPortalPackageNotificationService.acknowledgePackage(item.packageId(), true, null);
                log.info("Pacote {} enviado para {}", item.packageId(), item.submissionEmail());
            } catch (Exception ex) {
                log.error("Falha ao enviar pacote {} para {}", item.packageId(), item.submissionEmail(), ex);
                try {
                    leadPortalPackageNotificationService.acknowledgePackage(
                            item.packageId(), false, resolveRootCauseMessage(ex));
                } catch (Exception ackEx) {
                    log.error("Falha ao registrar a falha do pacote {}", item.packageId(), ackEx);
                }
            }
        }
    }

    private void sendEmail(LeadPortalImagePackageExportItem item) {
        if (!StringUtils.hasText(item.submissionEmail())) {
            throw new IllegalArgumentException("Destinatário vazio para o pacote " + item.packageId());
        }
        if (!StringUtils.hasText(item.emailSubject())) {
            throw new IllegalStateException("Conteúdo de e-mail ausente para o pacote " + item.packageId());
        }
        EmailLog emailLog = emailLogService.createPendingLog(
                item.submissionEmail(),
                item.emailSubject(),
                "lead-portal-package-" + item.packageId());

        PaymentBodies paymentBodies = enrichWithPaymentLink(
                item.emailHtmlBody(),
                item.emailPlainBody(),
                item);
        String trackingPixelUrl = trackingPixelService.buildTrackingPixelUrl(emailLog.getRequestId());
        String htmlBody = trackingPixelService.appendTrackingPixel(paymentBodies.htmlBody(), trackingPixelUrl);

        String fromAddress = smtpConfigurationService.resolveFromAddress();
        String fromName = smtpConfigurationService.resolveFromName();
        EmailMessage message = new EmailMessage(
                fromAddress,
                fromName,
                List.of(item.submissionEmail()),
                List.of(),
                List.of(),
                item.emailSubject(),
                htmlBody,
                paymentBodies.plainBody(),
                List.of()
        );

        Long purchaseId = item.paymentInfo() != null ? item.paymentInfo().purchaseId() : null;
        log.info("Enviando pacote {} para {} com assunto '{}' (purchaseId={})",
                item.packageId(),
                item.submissionEmail(),
                item.emailSubject(),
                purchaseId);

        try {
            emailSenderService.send(message);
            emailLogService.markSent(emailLog.getRequestId());
        } catch (Exception ex) {
            emailLogService.markFailed(emailLog.getRequestId(), ex.getMessage());
            throw ex;
        }
    }

    PaymentBodies enrichWithPaymentLink(String htmlBody,
                                       String plainBody,
                                       LeadPortalImagePackageExportItem item) {
        String html = htmlBody != null ? htmlBody : "";
        String plain = plainBody != null ? plainBody : "";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = item.paymentInfo();
        if (paymentInfo == null || !StringUtils.hasText(paymentInfo.checkoutUrl())) {
            return new PaymentBodies(html, plain);
        }

        String directPaymentUrl = paymentInfo.checkoutUrl().trim();
        String entrypointUrl = resolvePaymentUrl(item.packageId(), paymentInfo);
        if (StringUtils.hasText(entrypointUrl)) {
            entrypointUrl = entrypointUrl.trim();
        } else {
            entrypointUrl = directPaymentUrl;
        }

        String trackedCheckoutUrl = trackingLinkService.buildCheckoutTrackingUrl(paymentInfo.purchaseId(), item.submissionId())
                .map(String::trim)
                .orElse(null);
        String effectivePaymentUrl = StringUtils.hasText(trackedCheckoutUrl) ? trackedCheckoutUrl : entrypointUrl;

        validatePaymentUrl(effectivePaymentUrl);

        String updatedHtml = replacePaymentLinks(html, effectivePaymentUrl, entrypointUrl, directPaymentUrl);
        String updatedPlain = replacePaymentLinks(plain, effectivePaymentUrl, entrypointUrl, directPaymentUrl);

        boolean htmlHasLink = containsPaymentLink(updatedHtml, effectivePaymentUrl);
        boolean plainHasLink = containsPaymentLink(updatedPlain, effectivePaymentUrl);

        String normalizedHtml = htmlHasLink ? updatedHtml : updatedHtml + buildHtmlPaymentBlock(paymentInfo, effectivePaymentUrl);
        String normalizedPlain = plainHasLink ? updatedPlain : buildPlainPaymentBlock(updatedPlain, paymentInfo, effectivePaymentUrl);
        return new PaymentBodies(normalizedHtml, normalizedPlain);
    }

    private String resolvePaymentUrl(long packageId, LeadPortalImagePackageExportItem.PaymentInfo paymentInfo) {
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
        if (!paymentLinkProperties.isValidateHost() || !StringUtils.hasText(checkoutUrl)) {
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
                boolean isTrackingHost = trackingLinkService.trackingHost()
                        .map(trackingHost -> trackingHost.equalsIgnoreCase(host))
                        .orElse(false);
                if (!isTrackingHost) {
                    throw new IllegalArgumentException("Host do link de pagamento não autorizado: " + host);
                }
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL de pagamento inválida", ex);
        }
    }

    private String buildHtmlPaymentBlock(LeadPortalImagePackageExportItem.PaymentInfo paymentInfo, String paymentUrl) {
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
    private String buildPlainPaymentBlock(String plainBody, LeadPortalImagePackageExportItem.PaymentInfo paymentInfo, String paymentUrl) {
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
    private String replacePaymentLinks(String content, String finalUrl, String... candidates) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(finalUrl)) {
            return content;
        }
        String result = content;
        String trimmedFinalUrl = finalUrl.trim();
        String escapedFinal = HtmlUtils.htmlEscape(trimmedFinalUrl);
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            String trimmed = candidate.trim();
            String escapedCandidate = HtmlUtils.htmlEscape(trimmed);
            result = result.replace(trimmed, trimmedFinalUrl);
            if (!trimmed.equals(escapedCandidate)) {
                result = result.replace(escapedCandidate, escapedFinal);
            }
        }
        return result;
    }

    private boolean containsPaymentLink(String content, String... candidates) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String unescaped = HtmlUtils.htmlUnescape(content);
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            String trimmed = candidate.trim();
            String escaped = HtmlUtils.htmlEscape(trimmed);
            if (content.contains(trimmed) || content.contains(escaped) || unescaped.contains(trimmed)) {
                return true;
            }
        }
        return false;
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

    static record PaymentBodies(String htmlBody, String plainBody) {
    }

    private String resolveRootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return StringUtils.hasText(cursor.getMessage()) ? cursor.getMessage() : throwable.getMessage();
    }
}

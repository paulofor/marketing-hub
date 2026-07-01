package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.dto.DigitalProductDeliveryEmailRequest;
import com.marketinghub.emailservice.dto.EmailResponseDto;
import com.marketinghub.emailservice.model.EmailLog;
import com.marketinghub.emailservice.settings.EmailSmtpConfigurationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Monta e envia emails transacionais de entrega para produtos digitais comprados.
 */
@Service
public class DigitalProductDeliveryEmailService {

    private static final Logger log = LoggerFactory.getLogger(DigitalProductDeliveryEmailService.class);
    private static final String TEMPLATE_ID = "digital-product-delivery";

    private final EmailSenderService emailSenderService;
    private final EmailLogService emailLogService;
    private final EmailSmtpConfigurationService smtpConfigurationService;
    private final TrackingPixelService trackingPixelService;

    public DigitalProductDeliveryEmailService(EmailSenderService emailSenderService,
                                              EmailLogService emailLogService,
                                              EmailSmtpConfigurationService smtpConfigurationService,
                                              TrackingPixelService trackingPixelService) {
        this.emailSenderService = emailSenderService;
        this.emailLogService = emailLogService;
        this.smtpConfigurationService = smtpConfigurationService;
        this.trackingPixelService = trackingPixelService;
    }

    /** Envia o email pós-compra e registra o resultado em email_log. */
    public EmailResponseDto send(DigitalProductDeliveryEmailRequest request) {
        String subject = "Seu acesso ao " + request.productName();
        EmailLog emailLog = emailLogService.createPendingLog(request.to(), subject, TEMPLATE_ID);
        try {
            String trackingPixelUrl = trackingPixelService.buildTrackingPixelUrl(emailLog.getRequestId());
            String htmlBody = trackingPixelService.appendTrackingPixel(buildHtmlBody(request), trackingPixelUrl);
            String textBody = buildTextBody(request);
            EmailMessage message = new EmailMessage(
                    smtpConfigurationService.resolveFromAddress(),
                    smtpConfigurationService.resolveFromName(),
                    List.of(request.to()),
                    List.of(),
                    List.of(),
                    subject,
                    htmlBody,
                    textBody,
                    List.of());
            emailSenderService.send(message);
            EmailLog sentLog = emailLogService.markSent(emailLog.getRequestId());
            return new EmailResponseDto(sentLog.getRequestId(), sentLog.getStatus(), sentLog.getCreatedAt(),
                    sentLog.getSentAt(), "Email de entrega enviado");
        } catch (Exception ex) {
            log.error("Falha ao enviar email de entrega digital (paymentId={}, externalReference={}, to={})",
                    request.paymentId(), request.externalReference(), request.to(), ex);
            emailLogService.markFailed(emailLog.getRequestId(), ex.getMessage());
            throw ex;
        }
    }

    /** Constrói o HTML simples do email de entrega. */
    private String buildHtmlBody(DigitalProductDeliveryEmailRequest request) {
        String buyerGreeting = StringUtils.hasText(request.buyerName())
                ? "Olá, " + escape(request.buyerName()) + "."
                : "Olá.";
        String downloadFallback = StringUtils.hasText(request.downloadUrl())
                ? "<p>Se preferir, use este link direto de backup:<br><a href=\"" + escapeAttribute(request.downloadUrl())
                + "\">baixar o arquivo do produto</a>.</p>"
                : "";
        return """
                <!doctype html>
                <html lang="pt-BR">
                <body style="font-family:Arial,sans-serif;line-height:1.5;color:#202124">
                  <p>%s</p>
                  <p>Seu pagamento foi aprovado e o produto <strong>%s</strong> já está liberado.</p>
                  <p>
                    <a href="%s" style="display:inline-block;background:#0f7b4f;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:bold">
                      Acessar minha entrega
                    </a>
                  </p>
                  %s
                  <p>Comece pelo guia principal e depois use a planilha de agenda 7D.</p>
                  <p>Pagamento: %s<br>Referência: %s</p>
                </body>
                </html>
                """.formatted(
                buyerGreeting,
                escape(request.productName()),
                escapeAttribute(request.deliveryPageUrl()),
                downloadFallback,
                escape(request.paymentId()),
                escape(request.externalReference()));
    }

    /** Constrói o texto puro do email de entrega. */
    private String buildTextBody(DigitalProductDeliveryEmailRequest request) {
        String directLink = StringUtils.hasText(request.downloadUrl())
                ? "\n\nLink direto de backup: " + request.downloadUrl()
                : "";
        return """
                Seu pagamento foi aprovado e o produto %s já está liberado.

                Acesse sua entrega:
                %s%s

                Comece pelo guia principal e depois use a planilha de agenda 7D.

                Pagamento: %s
                Referência: %s
                """.formatted(
                request.productName(),
                request.deliveryPageUrl(),
                directLink,
                request.paymentId(),
                request.externalReference());
    }

    /** Escapa texto para uso seguro em HTML. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Escapa texto para uso seguro em atributos HTML. */
    private String escapeAttribute(String value) {
        return escape(value).replace("\"", "&quot;");
    }
}

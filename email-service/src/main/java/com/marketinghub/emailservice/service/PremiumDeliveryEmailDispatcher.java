package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.config.PremiumDeliveryProperties;
import com.marketinghub.emailservice.model.EmailLog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.StringUtils;

@Service
public class PremiumDeliveryEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PremiumDeliveryEmailDispatcher.class);

    private final PremiumDeliveryProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final EmailSenderService emailSenderService;
    private final EmailLogService emailLogService;
    private final EmailServiceProperties emailServiceProperties;
    private final TrackingPixelService trackingPixelService;

    public PremiumDeliveryEmailDispatcher(PremiumDeliveryProperties properties,
                                          JdbcTemplate jdbcTemplate,
                                          EmailSenderService emailSenderService,
                                          EmailLogService emailLogService,
                                          EmailServiceProperties emailServiceProperties,
                                          TrackingPixelService trackingPixelService) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.emailSenderService = emailSenderService;
        this.emailLogService = emailLogService;
        this.emailServiceProperties = emailServiceProperties;
        this.trackingPixelService = trackingPixelService;
    }

    @Scheduled(initialDelayString = "${premium-delivery.email.scheduler.initial-delay:25000}",
            fixedDelayString = "${premium-delivery.email.scheduler.delay:60000}")
    public void pollAndSendPremiumDeliveries() {
        if (!properties.isEnabled()) {
            return;
        }
        List<PremiumDeliveryRecord> records = findReadyDeliveries(Math.max(1, properties.getBatchSize()));
        for (PremiumDeliveryRecord record : records) {
            if (!lockForEmail(record.id())) {
                continue;
            }
            int attempt = record.emailAttempts() + 1;
            try {
                sendEmail(record);
            } catch (Exception ex) {
                log.error("Falha ao enviar imagens premium do pacote {} (delivery={})",
                        record.packageId(), record.id(), ex);
                recordEmailFailure(record.id(), record.purchaseId(), attempt, ex.getMessage());
            }
        }
    }

    private List<PremiumDeliveryRecord> findReadyDeliveries(int limit) {
        String sql = """
                SELECT
                    pd.id,
                    pd.package_id,
                    pd.purchase_id,
                    pd.recipient_email,
                    pd.recipient_name,
                    pd.zip_download_url,
                    pd.zip_object_key,
                    pd.email_attempts,
                    pd.email_last_attempt,
                    pd.email_last_error,
                    p.amount,
                    p.currency
                FROM lead_portal_premium_delivery pd
                JOIN lead_portal_purchase p ON p.id = pd.purchase_id
                WHERE pd.status IN ('ZIP_READY', 'EMAIL_SENDING')
                  AND pd.zip_object_key IS NOT NULL
                  AND (pd.email_last_attempt IS NULL
                       OR pd.email_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                  AND pd.email_attempts < ?
                ORDER BY pd.updated_at ASC, pd.id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRecord(rs),
                properties.getLockSeconds(), properties.getMaxAttempts(), limit);
    }

    private PremiumDeliveryRecord mapRecord(ResultSet rs) throws SQLException {
        return new PremiumDeliveryRecord(
                rs.getLong("id"),
                rs.getLong("package_id"),
                rs.getLong("purchase_id"),
                rs.getString("recipient_email"),
                rs.getString("recipient_name"),
                rs.getString("zip_download_url"),
                rs.getString("zip_object_key"),
                rs.getInt("email_attempts"),
                toInstant(rs.getTimestamp("email_last_attempt")),
                rs.getString("email_last_error"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"));
    }

    private boolean lockForEmail(long deliveryId) {
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    email_last_attempt = UTC_TIMESTAMP(),
                    email_attempts = email_attempts + 1,
                    email_last_error = NULL
                WHERE id = ?
                  AND status IN ('ZIP_READY', 'EMAIL_SENDING')
                  AND (email_last_attempt IS NULL OR email_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                """;
        int updated = jdbcTemplate.update(sql,
                PremiumDeliveryStatus.EMAIL_SENDING.name(),
                deliveryId,
                properties.getLockSeconds());
        return updated > 0;
    }

    private void sendEmail(PremiumDeliveryRecord record) {
        if (!StringUtils.hasText(record.recipientEmail())) {
            throw new IllegalStateException("Delivery " + record.id() + " sem destinatário");
        }
        String subject = "Suas imagens premium estão prontas";
        EmailLog logEntry = emailLogService.createPendingLog(
                record.recipientEmail(), subject, "premium-delivery-" + record.packageId());
        String trackingPixelUrl = trackingPixelService.buildTrackingPixelUrl(logEntry.getRequestId());
        String htmlBody = buildHtmlBody(record, trackingPixelUrl);
        htmlBody = trackingPixelService.appendTrackingPixel(htmlBody, trackingPixelUrl);
        String textBody = buildTextBody(record);

        EmailMessage message = new EmailMessage(
                emailServiceProperties.defaultFromAddress(),
                List.of(record.recipientEmail()),
                List.of(),
                List.of(),
                subject,
                htmlBody,
                textBody,
                List.of());

        emailSenderService.send(message);
        emailLogService.markSent(logEntry.getRequestId());
        markDeliverySent(record.id(), logEntry.getRequestId());
        markPurchaseDelivered(record.purchaseId());
        log.info("E-mail de originais enviado para {} (delivery={}, package={})",
                record.recipientEmail(), record.id(), record.packageId());
    }

    private String buildHtmlBody(PremiumDeliveryRecord record, String trackingPixelUrl) {
        String greeting = StringUtils.hasText(record.recipientName())
                ? "Olá, " + record.recipientName() + "!"
                : "Olá!";
        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"font-family:Arial,sans-serif;font-size:15px;color:#111;\">");
        builder.append("<p>").append(greeting).append("</p>");
        builder.append("<p>Recebemos o pagamento do seu pacote de imagens #")
                .append(record.packageId()).append(".</p>");
        if (StringUtils.hasText(record.downloadUrl())) {
            builder.append("<p>Use o botão abaixo para baixar todas as variações em alta resolução.</p>");
            builder.append("<p style=\"margin:20px 0;\"><a href=\"")
                    .append(HtmlUtils.htmlEscape(record.downloadUrl()))
                    .append("\" target=\"_blank\" rel=\"noopener\" style=\"background:#2563eb;color:#fff;padding:14px 26px;text-decoration:none;border-radius:6px;font-weight:600;\">Baixar imagens premium</a></p>");
        } else {
            builder.append("<p>Não conseguimos gerar o link automaticamente. Responda este e-mail e reenviamos os arquivos.</p>");
        }
        if (record.amount() != null && record.amount().signum() > 0) {
            builder.append("<p style=\"color:#555;font-size:14px;\">Pagamento confirmado: ")
                    .append(record.amount().setScale(2, java.math.RoundingMode.HALF_UP))
                    .append(' ')
                    .append(StringUtils.hasText(record.currency()) ? record.currency() : "").append(".</p>");
        }
        builder.append("<p style=\"color:#555;font-size:14px;\">Se precisar de ajuda é só responder este e-mail.</p>");
        builder.append("</div>");
        return builder.toString();
    }

    private String buildTextBody(PremiumDeliveryRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("Olá");
        if (StringUtils.hasText(record.recipientName())) {
            builder.append(", ").append(record.recipientName());
        }
        builder.append("!\n\n");
        builder.append("Recebemos o pagamento do seu pacote de imagens #")
                .append(record.packageId()).append(".\n");
        if (StringUtils.hasText(record.downloadUrl())) {
            builder.append("Baixe as imagens neste link: ")
                    .append(record.downloadUrl())
                    .append("\n\n");
        } else {
            builder.append("Não conseguimos gerar o link automaticamente. Responda este e-mail para recebermos os arquivos.\n\n");
        }
        builder.append("Se precisar de ajuda é só responder esta mensagem.\n");
        return builder.toString();
    }

    private void markDeliverySent(long deliveryId, String requestId) {
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    email_sent_at = UTC_TIMESTAMP(),
                    email_request_id = ?,
                    email_last_error = NULL
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                PremiumDeliveryStatus.DELIVERED.name(),
                requestId,
                deliveryId);
    }

    private void markPurchaseDelivered(long purchaseId) {
        String sql = """
                UPDATE lead_portal_purchase
                SET status = 'DELIVERED',
                    delivered_at = UTC_TIMESTAMP(),
                    delivery_error = NULL
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, purchaseId);
    }

    private void recordEmailFailure(long deliveryId, long purchaseId, int attempts, String errorMessage) {
        boolean exhausted = attempts >= properties.getMaxAttempts();
        String status = exhausted ? PremiumDeliveryStatus.FAILED.name() : PremiumDeliveryStatus.ZIP_READY.name();
        String message = truncate(errorMessage, 500);
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    email_last_error = ?,
                    email_last_attempt = UTC_TIMESTAMP()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, status, message, deliveryId);
        jdbcTemplate.update("UPDATE lead_portal_purchase SET delivery_error = ? WHERE id = ?", message, purchaseId);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String truncate(String value, int length) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private record PremiumDeliveryRecord(long id,
                                         long packageId,
                                         long purchaseId,
                                         String recipientEmail,
                                         String recipientName,
                                         String downloadUrl,
                                         String objectKey,
                                         int emailAttempts,
                                         Instant lastAttempt,
                                         String lastError,
                                         java.math.BigDecimal amount,
                                         String currency) {
    }
}

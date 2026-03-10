package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient.PaymentCheckoutResponse;
import com.marketinghub.storage.FileStorageService;
import com.marketinghub.storage.StorageException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Scans completed lead portal image packages and dispatches e-mails with watermarked previews.
 */
@Service
public class LeadPortalPackageNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPackageNotificationService.class);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(new Locale("pt", "BR"))
            .withZone(ZoneId.systemDefault());

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;
    private final LeadPortalImagePackageStatusHistoryService statusHistoryService;
    private final LeadPortalPaymentsClient paymentsClient;

    @Value("${lead-portal.notifications.max-attempts:5}")
    private int maxAttempts;

    @Value("${lead-portal.notifications.batch-size:3}")
    private int batchSize;

    @Value("${lead-portal.notifications.lock-seconds:300}")
    private int lockSeconds;

    @Value("${lead-portal.notifications.tracking-base-url:}")
    private String trackingBaseUrl;

    public LeadPortalPackageNotificationService(
            JdbcTemplate jdbcTemplate,
            FileStorageService fileStorageService,
            LeadPortalImagePackageStatusHistoryService statusHistoryService,
            LeadPortalPaymentsClient paymentsClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.statusHistoryService = statusHistoryService;
        this.paymentsClient = paymentsClient;
    }

    @PostConstruct
    void logConfiguration() {
        log.info("Lead portal package export ready (maxAttempts={}, batchSize={})", maxAttempts, batchSize);
    }

    public java.util.List<LeadPortalImagePackageExportItem> exportReadyPackages(int limit) {
        java.util.List<PendingPackage> packages = findPendingPackages(limit <= 0 ? batchSize : limit);
        java.util.List<LeadPortalImagePackageExportItem> exports = new java.util.ArrayList<>();
        for (PendingPackage pending : packages) {
            if (!lockPackage(pending.packageId())) {
                continue;
            }
            try {
                exports.add(buildExportItem(pending));
            } catch (Exception ex) {
                log.error("Failed to prepare export for lead portal package {}", pending.packageId(), ex);
                recordFailure(pending.packageId(), ex.getMessage());
            }
        }
        return exports;
    }

    private LeadPortalImagePackageExportItem buildExportItem(PendingPackage pending) throws IOException {
        if (!StringUtils.hasText(pending.zipObjectKey())) {
            throw new IllegalStateException("ZIP ainda não gerado para o pacote " + pending.packageId());
        }
        int imageCount = pending.imageCount() > 0 ? pending.imageCount() : countWatermarkedAssets(pending.packageId());
        byte[] zipBytes = loadObjectBytes(pending.zipObjectKey());
        PaymentInfo paymentInfo = ensurePaymentInfo(pending);
        EmailContent content = buildEmailContent(pending, imageCount, paymentInfo);
        return new LeadPortalImagePackageExportItem(
                pending.packageId(),
                pending.submissionId(),
                pending.submissionName(),
                pending.submissionEmail(),
                pending.status(),
                pending.experimentId(),
                pending.experimentName(),
                pending.sampleSubject(),
                pending.samplePreview(),
                pending.sampleBody(),
                pending.sampleCallToAction(),
                pending.sampleModel(),
                pending.samplePrompt(),
                pending.sampleUpdatedAt(),
                pending.notificationAttempts(),
                pending.lastAttempt(),
                zipBytes,
                pending.zipObjectKey(),
                content.attachmentName(),
                imageCount,
                content.subject(),
                content.plain(),
                content.html(),
                new LeadPortalImagePackageExportItem.PaymentInfo(
                        paymentInfo.purchaseId(),
                        paymentInfo.checkoutUrl(),
                        paymentInfo.amount(),
                        paymentInfo.currency(),
                        paymentInfo.expiresAt(),
                        paymentInfo.statementDescriptor()));
    }

    private boolean lockPackage(long packageId) {
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notification_last_attempt = ?, notification_last_error = NULL WHERE id = ? "
                        + "AND (notification_last_attempt IS NULL OR notification_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))",
                Timestamp.from(Instant.now()),
                packageId,
                lockSeconds);
        return updated > 0;
    }

    public void acknowledgePackage(long packageId, boolean success, String errorMessage) {
        if (success) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacote de imagem não encontrado"));
            markAsNotified(packageId, current);
        } else {
            recordFailure(packageId, errorMessage != null ? errorMessage : "Falha desconhecida");
        }
    }

    private List<PendingPackage> findPendingPackages(int limit) {
        String sql = """
                SELECT
                    pack.id AS package_id,
                    pack.submission_id,
                    pack.status,
                    pack.notification_attempts,
                    pack.notification_last_attempt,
                    pack.zip_object_key,
                    pack.zip_size_bytes,
                    pack.zip_generated_at,
                    (
                        SELECT COUNT(*) FROM flow_submission_image_watermark wm
                          JOIN flow_submission_image_item items2 ON items2.id = wm.item_id
                        WHERE items2.package_id = pack.id
                    ) AS watermarked_count,
                    sub.name AS submission_name,
                    sub.email AS submission_email,
                    exp.id AS experiment_id,
                    COALESCE(exp.name, flow.name, flow.slug) AS experiment_name,
                    sample.subject AS sample_subject,
                    sample.preview_text AS sample_preview,
                    sample.body AS sample_body,
                    sample.call_to_action AS sample_call_to_action,
                    sample.model AS sample_model,
                    sample.prompt AS sample_prompt,
                    sample.updated_at AS sample_updated_at,
                    pack.payment_purchase_id,
                    pack.payment_checkout_url,
                    pack.payment_checkout_expires_at,
                    pack.payment_amount,
                    pack.payment_currency,
                    pack.payment_statement_descriptor
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                LEFT JOIN experiment_sample_email sample ON sample.id = exp.selected_sample_email_id
                WHERE sub.email IS NOT NULL AND sub.email <> ''
                  AND pack.notified_at IS NULL
                  AND pack.zip_object_key IS NOT NULL
                  AND pack.zip_generated_at IS NOT NULL
                  AND (pack.notification_last_attempt IS NULL
                       OR pack.notification_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                  AND pack.notification_attempts < ?
                  AND pack.status <> 'FAILED'
                  AND (
                        SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id
                      ) > 0
                  AND (
                        SELECT COUNT(*) FROM flow_submission_image_watermark wm
                          JOIN flow_submission_image_item items2 ON items2.id = wm.item_id
                        WHERE items2.package_id = pack.id
                      ) >= (
                        SELECT COUNT(*) FROM flow_submission_image_item items WHERE items.package_id = pack.id
                      )
                ORDER BY pack.updated_at ASC, pack.id ASC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPendingPackage(rs), lockSeconds, maxAttempts, limit);
    }

    private PendingPackage mapPendingPackage(ResultSet rs) throws SQLException {
        return new PendingPackage(
                rs.getLong("package_id"),
                normalizeSubmissionId(rs.getObject("submission_id")),
                FlowSubmissionImagePackageStatus.valueOf(rs.getString("status")),
                rs.getInt("notification_attempts"),
                toInstant(rs.getTimestamp("notification_last_attempt")),
                rs.getString("submission_name"),
                rs.getString("submission_email"),
                (Long) rs.getObject("experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("sample_subject"),
                rs.getString("sample_preview"),
                rs.getString("sample_body"),
                rs.getString("sample_call_to_action"),
                rs.getString("sample_model"),
                rs.getString("sample_prompt"),
                toInstant(rs.getTimestamp("sample_updated_at")),
                rs.getString("zip_object_key"),
                rs.getLong("zip_size_bytes"),
                toInstant(rs.getTimestamp("zip_generated_at")),
                rs.getInt("watermarked_count"),
                (Long) rs.getObject("payment_purchase_id"),
                rs.getString("payment_checkout_url"),
                toInstant(rs.getTimestamp("payment_checkout_expires_at")),
                rs.getBigDecimal("payment_amount"),
                rs.getString("payment_currency"),
                rs.getString("payment_statement_descriptor")
        );
    }

    private int countWatermarkedAssets(long packageId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flow_submission_image_watermark wm JOIN flow_submission_image_item item ON item.id = wm.item_id WHERE item.package_id = ?",
                Integer.class,
                packageId);
        return count != null ? count : 0;
    }

    private byte[] loadObjectBytes(String storedFileName) throws IOException {
        if (!StringUtils.hasText(storedFileName)) {
            throw new IOException("Nome do arquivo do bucket não informado");
        }
        try {
            try (InputStream in = fileStorageService.loadAsResource(storedFileName).getInputStream()) {
                return in.readAllBytes();
            }
        } catch (StorageException ex) {
            throw new IOException("Failed to load object '" + storedFileName + "'", ex);
        }
    }

    private EmailContent buildEmailContent(PendingPackage pending, int imageCount, PaymentInfo paymentInfo) {
        String subject = resolveEmailSubject(pending, imageCount);
        String normalizedTrackingBase = normalizeTrackingBaseUrl(trackingBaseUrl);
        String trackingViewUrl = null;
        String trackingPixelUrl = null;
        if (StringUtils.hasText(normalizedTrackingBase)) {
            trackingViewUrl = buildTrackingUrl(normalizedTrackingBase, pending.packageId(), "previews", pending.submissionId());
            trackingPixelUrl = buildTrackingUrl(normalizedTrackingBase, pending.packageId(), "open.gif", pending.submissionId());
        }
        StringBuilder plain = new StringBuilder();
        if (StringUtils.hasText(pending.samplePreview())) {
            plain.append(pending.samplePreview()).append("\n\n");
        }
        if (StringUtils.hasText(pending.sampleBody())) {
            plain.append(pending.sampleBody().trim()).append("\n\n");
        }
        plain.append("Anexamos ").append(imageCount).append(" imagem(ns) com marca d'água geradas para este lead.\n");
        if (StringUtils.hasText(pending.sampleCallToAction())) {
            plain.append("CTA sugerido: ").append(pending.sampleCallToAction().trim()).append("\n");
        }
        if (trackingViewUrl != null) {
            plain.append("Acesse as prévias online: ").append(trackingViewUrl).append("\n");
        }
        plain.append("\nID do pacote: ").append(pending.packageId());
        if (pending.sampleUpdatedAt() != null) {
            plain.append("\nE-mail atualizado em: ").append(HUMAN_DATE.format(pending.sampleUpdatedAt()));
        }

        StringBuilder html = new StringBuilder();
        if (StringUtils.hasText(pending.samplePreview())) {
            html.append("<p><em>").append(HtmlUtils.htmlEscape(pending.samplePreview().trim())).append("</em></p>");
        }
        if (StringUtils.hasText(pending.sampleBody())) {
            String[] paragraphs = pending.sampleBody().split("\\n{2,}");
            for (String paragraph : paragraphs) {
                html.append("<p>").append(HtmlUtils.htmlEscape(paragraph.trim()).replace("\n", "<br/>"))
                        .append("</p>");
            }
        }
        html.append("<p><strong>").append(imageCount)
                .append(" imagem(ns) com marca d'água estão anexadas a este e-mail.</strong></p>");
        if (StringUtils.hasText(pending.sampleCallToAction())) {
            html.append("<p><strong>CTA sugerido:</strong> ")
                    .append(HtmlUtils.htmlEscape(pending.sampleCallToAction().trim()))
                    .append("</p>");
        }

        appendPaymentCallToAction(plain, html, paymentInfo, pending.sampleCallToAction());

        if (StringUtils.hasText(trackingViewUrl)) {
            html.append("<p><strong>Visualizar online:</strong> <a href=\"")
                    .append(HtmlUtils.htmlEscape(trackingViewUrl))
                    .append("\" target=\"_blank\" rel=\"noopener\">Abrir prévias</a></p>");
        }
        html.append("<p class=\"meta\" style=\"font-size:12px;color:#555\">")
                .append("Pacote ").append(pending.packageId())
                .append(" · Experimento ")
                .append(HtmlUtils.htmlEscape(resolveExperimentLabel(pending)))
                .append("</p>");
        if (StringUtils.hasText(trackingPixelUrl)) {
            html.append("<img src=\"")
                    .append(HtmlUtils.htmlEscape(trackingPixelUrl))
                    .append("\" alt=\"\" width=\"1\" height=\"1\" style=\"display:none;\" />");
        }

        String attachmentName = "imagens-watermark-" + pending.packageId() + ".zip";
        return new EmailContent(subject, plain.toString(), html.toString(), attachmentName);
    }

    private String resolveEmailSubject(PendingPackage pending, int imageCount) {
        if (StringUtils.hasText(pending.sampleSubject())) {
            return pending.sampleSubject().trim();
        }
        String experimentLabel = resolveExperimentLabel(pending);
        boolean singleImage = imageCount == 1;
        String readySnippet = singleImage
                ? "sua imagem com marca d'água está pronta"
                : imageCount + " imagens com marca d'água estão prontas";
        if (StringUtils.hasText(pending.submissionName())) {
            return pending.submissionName().trim() + ", " + readySnippet;
        }
        if (StringUtils.hasText(experimentLabel)) {
            return "Prévia " + experimentLabel + ": " + readySnippet;
        }
        return singleImage
                ? "Sua imagem com marca d'água está pronta"
                : "Suas " + imageCount + " imagens com marca d'água estão prontas";
    }

    private String resolveExperimentLabel(PendingPackage pending) {
        if (StringUtils.hasText(pending.experimentName())) {
            return pending.experimentName().trim();
        }
        return "Lead Portal";
    }

    private PaymentInfo ensurePaymentInfo(PendingPackage pending) {
        if (isPaymentLinkValid(pending.paymentCheckoutUrl(), pending.paymentCheckoutExpiresAt())) {
            return new PaymentInfo(
                    pending.paymentPurchaseId(),
                    pending.paymentCheckoutUrl(),
                    pending.paymentAmount(),
                    pending.paymentCurrency(),
                    pending.paymentCheckoutExpiresAt(),
                    pending.paymentStatementDescriptor());
        }
        PaymentCheckoutResponse checkout = paymentsClient.ensureCheckout(
                pending.packageId(), pending.submissionEmail(), pending.submissionName());
        if (checkout == null || !StringUtils.hasText(checkout.checkoutUrl())) {
            throw new IllegalStateException("Serviço de pagamentos não retornou link válido para o pacote "
                    + pending.packageId());
        }
        persistPaymentInfo(pending.packageId(), checkout);
        return new PaymentInfo(
                checkout.purchaseId(),
                checkout.checkoutUrl(),
                checkout.amount(),
                checkout.currency(),
                checkout.expiresAt(),
                checkout.statementDescriptor());
    }

    private boolean isPaymentLinkValid(String checkoutUrl, Instant expiresAt) {
        if (!StringUtils.hasText(checkoutUrl)) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.isAfter(Instant.now());
    }

    private void persistPaymentInfo(long packageId, PaymentCheckoutResponse checkout) {
        Timestamp expiresAt = checkout.expiresAt() != null ? Timestamp.from(checkout.expiresAt()) : null;
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET payment_purchase_id = ?, payment_checkout_url = ?, "
                        + "payment_checkout_expires_at = ?, payment_amount = ?, payment_currency = ?, "
                        + "payment_statement_descriptor = ?, updated_at = ? WHERE id = ?",
                checkout.purchaseId(),
                checkout.checkoutUrl(),
                expiresAt,
                checkout.amount(),
                checkout.currency(),
                checkout.statementDescriptor(),
                Timestamp.from(Instant.now()),
                packageId);
    }

    private void appendPaymentCallToAction(StringBuilder plain,
                                           StringBuilder html,
                                           PaymentInfo paymentInfo,
                                           String suggestedCta) {
        if (paymentInfo == null || !StringUtils.hasText(paymentInfo.checkoutUrl())) {
            return;
        }
        String paymentUrl = paymentInfo.checkoutUrl().trim();
        String formattedAmount = formatCurrency(paymentInfo.amount(), paymentInfo.currency());
        String descriptor = StringUtils.hasText(paymentInfo.statementDescriptor())
                ? paymentInfo.statementDescriptor()
                : "Mercado Pago";

        plain.append("Finalize o pagamento e libere as imagens originais:\n")
                .append(paymentUrl)
                .append("\n");
        if (formattedAmount != null) {
            plain.append("Valor: ").append(formattedAmount).append("\n");
        }
        plain.append("Processado por ").append(descriptor).append("\n\n");

        String ctaLabel = StringUtils.hasText(suggestedCta)
                ? suggestedCta.trim()
                : "Quero liberar as imagens originais";
        if (formattedAmount != null) {
            ctaLabel = ctaLabel + " — " + formattedAmount;
        }

        html.append("<div style=\"margin:24px 0;padding:16px 20px;border:1px solid #e3e3e3;border-radius:8px;background:#f8f8f8;\">")
                .append("<p><strong>Finalize o pagamento para liberar os arquivos originais.</strong></p>")
                .append("<p>Processado por ")
                .append(HtmlUtils.htmlEscape(descriptor))
                .append("</p>")
                .append("<p style=\"text-align:center;\"><a href=\"")
                .append(HtmlUtils.htmlEscape(paymentUrl))
                .append("\" target=\"_blank\" rel=\"noopener\" style=\"display:inline-block;padding:14px 28px;background:#00a650;color:#fff;font-weight:600;border-radius:6px;text-decoration:none;\">")
                .append(HtmlUtils.htmlEscape(ctaLabel))
                .append("</a></p>")
                .append("</div>");
    }
    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        Locale locale = "BRL".equalsIgnoreCase(currency) ? new Locale("pt", "BR") : Locale.US;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        try {
            return formatter.format(amount);
        } catch (IllegalArgumentException ignored) {
            return amount.toPlainString() + (StringUtils.hasText(currency) ? " " + currency : "");
        }
    }

    private record PaymentInfo(
            Long purchaseId,
            String checkoutUrl,
            BigDecimal amount,
            String currency,
            Instant expiresAt,
            String statementDescriptor) {
    }


    private String normalizeTrackingBaseUrl(String rawBaseUrl) {
        if (!StringUtils.hasText(rawBaseUrl)) {
            return null;
        }
        String trimmed = rawBaseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildTrackingUrl(String baseUrl, long packageId, String suffix, String submissionId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/public/lead-portal/image-packages/")
                .path(String.valueOf(packageId))
                .path("/")
                .path(suffix);
        if (StringUtils.hasText(submissionId)) {
            builder.queryParam("sid", submissionId);
        }
        return builder.toUriString();
    }

    private void markAsNotified(long packageId, FlowSubmissionImagePackageStatus currentStatus) {
        FlowSubmissionImagePackageStatus nextStatus = currentStatus == FlowSubmissionImagePackageStatus.COMPLETED
                ? currentStatus
                : FlowSubmissionImagePackageStatus.COMPLETED;
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notified_at = ?, notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = NULL, status = ? WHERE id = ?",
                Timestamp.from(now),
                Timestamp.from(now),
                nextStatus.name(),
                packageId);
        if (updated > 0) {
            statusHistoryService.recordStatusChange(packageId, nextStatus, null, now);
        }
    }

    private void recordFailure(long packageId, String error) {
        String normalizedError = normalizeError(error);
        Instant now = Instant.now();

        int updatedAttempts = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = ? WHERE id = ?",
                Timestamp.from(now),
                normalizedError,
                packageId);
        if (updatedAttempts == 0) {
            log.warn("Pacote {} não encontrado ao registrar falha de notificação", packageId);
            return;
        }

        NotificationStatus notificationStatus = findNotificationStatus(packageId).orElse(null);
        if (notificationStatus == null) {
            log.warn("Pacote {} não encontrado ao carregar tentativas de notificação após falha", packageId);
            return;
        }

        int attempts = notificationStatus.notificationAttempts() != null
                ? notificationStatus.notificationAttempts()
                : 0;
        int allowedAttempts = Math.max(1, maxAttempts);
        if (attempts >= allowedAttempts
                && notificationStatus.status() != FlowSubmissionImagePackageStatus.FAILED) {
            String failureReason = buildEmailFailureReason(normalizedError);
            int updated = jdbcTemplate.update(
                    "UPDATE flow_submission_image_package SET status = ?, failure_reason = ?, updated_at = ? WHERE id = ?",
                    FlowSubmissionImagePackageStatus.FAILED.name(),
                    failureReason,
                    Timestamp.from(now),
                    packageId);
            if (updated > 0) {
                statusHistoryService.recordStatusChange(
                        packageId, FlowSubmissionImagePackageStatus.FAILED, failureReason, now);
                log.warn(
                        "Pacote {} marcado como FAILED após {} tentativas de envio de e-mail: {}",
                        packageId,
                        attempts,
                        normalizedError);
            }
        }
    }

    private Optional<FlowSubmissionImagePackageStatus> findStatus(long packageId) {
        String sql = "SELECT status FROM flow_submission_image_package WHERE id = ?";
        List<FlowSubmissionImagePackageStatus> statuses = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> parseStatus(rs.getString("status")),
                packageId);
        return statuses.stream().findFirst();
    }

    private FlowSubmissionImagePackageStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Optional<NotificationStatus> findNotificationStatus(long packageId) {
        String sql = "SELECT status, notification_attempts FROM flow_submission_image_package WHERE id = ?";
        List<NotificationStatus> statuses = jdbcTemplate.query(sql, (rs, rowNum) -> new NotificationStatus(
                parseStatus(rs.getString("status")),
                (Integer) rs.getObject("notification_attempts")), packageId);
        return statuses.stream().findFirst();
    }


    private String normalizeError(String rawError) {
        String value = StringUtils.hasText(rawError)
                ? rawError.trim()
                : "Motivo não informado pelo serviço de e-mail";
        if (value.length() > 500) {
            value = value.substring(0, 500);
        }
        return value;
    }

    private String normalizeSubmissionId(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof byte[] bytes) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                return new UUID(buffer.getLong(), buffer.getLong()).toString();
            } catch (Exception ex) {
                log.warn("Falha ao converter submission_id binário para UUID", ex);
                return null;
            }
        }
        String value = rawValue.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    private String buildEmailFailureReason(String normalizedError) {
        String reason = "Falha ao enviar e-mail: " + normalizedError;
        if (reason.length() > 500) {
            return reason.substring(0, 500);
        }
        return reason;
    }

    private record PendingPackage(
            long packageId,
            String submissionId,
            FlowSubmissionImagePackageStatus status,
            int notificationAttempts,
            Instant lastAttempt,
            String submissionName,
            String submissionEmail,
            Long experimentId,
            String experimentName,
            String sampleSubject,
            String samplePreview,
            String sampleBody,
            String sampleCallToAction,
            String sampleModel,
            String samplePrompt,
            Instant sampleUpdatedAt,
            String zipObjectKey,
            Long zipSizeBytes,
            Instant zipGeneratedAt,
            int imageCount,
            Long paymentPurchaseId,
            String paymentCheckoutUrl,
            Instant paymentCheckoutExpiresAt,
            BigDecimal paymentAmount,
            String paymentCurrency,
            String paymentStatementDescriptor) {
    }

    private record EmailContent(String subject, String plain, String html, String attachmentName) {
    }

    private record NotificationStatus(
            FlowSubmissionImagePackageStatus status,
            Integer notificationAttempts) {
    }

}

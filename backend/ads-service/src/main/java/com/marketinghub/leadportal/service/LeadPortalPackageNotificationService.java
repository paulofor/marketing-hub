package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.payments.LeadPortalPaymentLinkService;
import com.marketinghub.storage.FileStorageService;
import com.marketinghub.storage.StorageException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import org.springframework.scheduling.annotation.Scheduled;
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
    private final LeadPortalEmailSender emailSender;
    private final LeadPortalImagePackageStatusHistoryService statusHistoryService;
    private final LeadPortalPaymentLinkService paymentLinkService;

    @Value("${lead-portal.notifications.enabled:false}")
    private boolean notificationsEnabled;

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
            LeadPortalEmailSender emailSender,
            LeadPortalImagePackageStatusHistoryService statusHistoryService,
            LeadPortalPaymentLinkService paymentLinkService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.emailSender = emailSender;
        this.statusHistoryService = statusHistoryService;
        this.paymentLinkService = paymentLinkService;
    }

    @PostConstruct
    void logConfiguration() {
        log.info("Lead portal notification service enabled: {} (maxAttempts={}, batchSize={})",
                notificationsEnabled, maxAttempts, batchSize);
    }

    @Scheduled(initialDelayString = "${lead-portal.notifications.initial-delay:20000}",
            fixedDelayString = "${lead-portal.notifications.poll-interval:60000}")
    public void dispatchReadyPackages() {
        if (!notificationsEnabled) {
            return;
        }
        List<PendingPackage> packages = findPendingPackages(batchSize);
        for (PendingPackage pending : packages) {
            if (!lockPackage(pending.packageId())) {
                continue;
            }
            try {
                processPackage(pending);
            } catch (Exception ex) {
                log.error("Failed to dispatch lead portal package {}", pending.packageId(), ex);
                recordFailure(pending.packageId(), ex.getMessage());
            }
        }
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
        EmailContent content = buildEmailContent(pending, imageCount);
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
                content.html());
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
                    exp.name AS experiment_name,
                    sample.subject AS sample_subject,
                    sample.preview_text AS sample_preview,
                    sample.body AS sample_body,
                    sample.call_to_action AS sample_call_to_action,
                    sample.model AS sample_model,
                    sample.prompt AS sample_prompt,
                    sample.updated_at AS sample_updated_at
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                JOIN experiment_sample_email sample ON sample.id = exp.selected_sample_email_id
                WHERE exp.selected_sample_email_id IS NOT NULL
                  AND sub.email IS NOT NULL AND sub.email <> ''
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
                rs.getLong("experiment_id"),
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
                rs.getInt("watermarked_count")
        );
    }

    private void processPackage(PendingPackage pending) throws IOException {
        if (!StringUtils.hasText(pending.zipObjectKey())) {
            throw new IllegalStateException("ZIP ausente para o pacote " + pending.packageId());
        }
        int imageCount = pending.imageCount() > 0 ? pending.imageCount() : countWatermarkedAssets(pending.packageId());
        byte[] zipBytes = loadObjectBytes(pending.zipObjectKey());

        EmailContent content = buildEmailContent(pending, imageCount);
        emailSender.sendEmail(
                pending.submissionEmail(),
                content.subject(),
                content.plain(),
                content.html(),
                zipBytes,
                content.attachmentName());

        markAsNotified(pending.packageId(), pending.status());
        log.info("Dispatched lead portal package {} to {} (zip: {}, {} bytes)", pending.packageId(), pending.submissionEmail(), pending.zipObjectKey(), pending.zipSizeBytes());
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

    private EmailContent buildEmailContent(PendingPackage pending, int imageCount) {
        String subject = pending.sampleSubject();
        String paymentLink = paymentLinkService.resolveCheckoutLink(
                pending.packageId(),
                pending.submissionEmail(),
                pending.submissionName())
                .orElse(null);
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
        if (StringUtils.hasText(paymentLink)) {
            plain.append("Para receber as imagens originais sem marca d'água, finalize o pagamento com segurança no Mercado Pago: ")
                    .append(paymentLink)
                    .append("\n\n");
        }
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
        if (StringUtils.hasText(paymentLink)) {
            html.append("<p><strong>Receber imagens originais:</strong> <a href=\"")
                    .append(HtmlUtils.htmlEscape(paymentLink))
                    .append("\" target=\"_blank\" rel=\"noopener\">Pagar com Mercado Pago</a></p>");
        }
        if (StringUtils.hasText(pending.sampleCallToAction())) {
            html.append("<p><strong>CTA sugerido:</strong> ")
                    .append(HtmlUtils.htmlEscape(pending.sampleCallToAction().trim()))
                    .append("</p>");
        }
        if (StringUtils.hasText(trackingViewUrl)) {
            html.append("<p><strong>Visualizar online:</strong> <a href=\"")
                    .append(HtmlUtils.htmlEscape(trackingViewUrl))
                    .append("\" target=\"_blank\" rel=\"noopener\">Abrir prévias</a></p>");
        }
        html.append("<p class=\"meta\" style=\"font-size:12px;color:#555\">")
                .append("Pacote ").append(pending.packageId())
                .append(" · Experimento ")
                .append(HtmlUtils.htmlEscape(pending.experimentName()))
                .append("</p>");
        if (StringUtils.hasText(trackingPixelUrl)) {
            html.append("<img src=\"")
                    .append(HtmlUtils.htmlEscape(trackingPixelUrl))
                    .append("\" alt=\"\" width=\"1\" height=\"1\" style=\"display:none;\" />");
        }

        String attachmentName = "imagens-watermark-" + pending.packageId() + ".zip";
        return new EmailContent(subject, plain.toString(), html.toString(), attachmentName);
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
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notified_at = ?, notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = NULL, status = ? WHERE id = ?",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                nextStatus.name(),
                packageId);
        if (updated > 0) {
            statusHistoryService.recordStatusChange(packageId, nextStatus, null);
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
                        packageId, FlowSubmissionImagePackageStatus.FAILED, failureReason);
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
            long experimentId,
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
            int imageCount) {
    }

    private record EmailContent(String subject, String plain, String html, String attachmentName) {
    }

    private record NotificationStatus(
            FlowSubmissionImagePackageStatus status,
            Integer notificationAttempts) {
    }

}

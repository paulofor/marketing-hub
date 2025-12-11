package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.storage.FileStorageService;
import com.marketinghub.storage.StorageException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

    @Value("${lead-portal.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${lead-portal.notifications.max-attempts:5}")
    private int maxAttempts;

    @Value("${lead-portal.notifications.batch-size:3}")
    private int batchSize;

    @Value("${lead-portal.notifications.lock-seconds:300}")
    private int lockSeconds;

    public LeadPortalPackageNotificationService(
            JdbcTemplate jdbcTemplate,
            FileStorageService fileStorageService,
            LeadPortalEmailSender emailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.emailSender = emailSender;
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
        List<WatermarkedAsset> assets = fetchWatermarkedAssets(pending.packageId());
        if (assets.isEmpty()) {
            throw new IllegalStateException("No watermarked assets found for package " + pending.packageId());
        }
        byte[] zipBytes = buildZipArchive(assets);
        EmailContent content = buildEmailContent(pending, assets.size());
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
                content.attachmentName(),
                assets.size(),
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

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPendingPackage(rs), maxAttempts, lockSeconds, limit);
    }

    private PendingPackage mapPendingPackage(ResultSet rs) throws SQLException {
        return new PendingPackage(
                rs.getLong("package_id"),
                rs.getString("submission_id"),
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
                toInstant(rs.getTimestamp("sample_updated_at"))
        );
    }

    private List<WatermarkedAsset> fetchWatermarkedAssets(long packageId) {
        String sql = """
                SELECT
                    COALESCE(wm_asset.external_id, wm_asset.url) AS stored_file_name,
                    wm_asset.id AS asset_id,
                    item.position_index
                FROM flow_submission_image_item item
                JOIN flow_submission_image_watermark wm ON wm.item_id = item.id
                JOIN asset wm_asset ON wm_asset.id = wm.asset_id
                WHERE item.package_id = ?
                ORDER BY item.position_index ASC, item.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new WatermarkedAsset(
                rs.getString("stored_file_name"),
                rs.getLong("asset_id"),
                rs.getInt("position_index")), packageId);
    }

    private void processPackage(PendingPackage pending) throws IOException {
        List<WatermarkedAsset> assets = fetchWatermarkedAssets(pending.packageId());
        if (assets.isEmpty()) {
            throw new IllegalStateException("No watermarked assets found for package " + pending.packageId());
        }

        byte[] zipBytes = buildZipArchive(assets);

        EmailContent content = buildEmailContent(pending, assets.size());
        emailSender.sendEmail(
                pending.submissionEmail(),
                content.subject(),
                content.plain(),
                content.html(),
                zipBytes,
                content.attachmentName());

        markAsNotified(pending.packageId(), pending.status());
        log.info("Dispatched lead portal package {} to {}", pending.packageId(), pending.submissionEmail());
    }

    private byte[] buildZipArchive(List<WatermarkedAsset> assets) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int index = 1;
            for (WatermarkedAsset asset : assets) {
                byte[] bytes = loadAssetBytes(asset.storedFileName());
                String extension = resolveExtension(asset.storedFileName());
                String entryName = String.format(Locale.ROOT, "imagem-%02d%s", index, extension);
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(bytes);
                zos.closeEntry();
                index++;
            }
        }
        return baos.toByteArray();
    }

    private byte[] loadAssetBytes(String storedFileName) throws IOException {
        try {
            try (InputStream in = fileStorageService.loadAsResource(storedFileName).getInputStream()) {
                return in.readAllBytes();
            }
        } catch (StorageException ex) {
            throw new IOException("Failed to load asset '" + storedFileName + "'", ex);
        }
    }

    private EmailContent buildEmailContent(PendingPackage pending, int imageCount) {
        String subject = pending.sampleSubject();
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
        html.append("<p class=\"meta\" style=\"font-size:12px;color:#555\">")
                .append("Pacote ").append(pending.packageId())
                .append(" · Experimento ")
                .append(HtmlUtils.htmlEscape(pending.experimentName()))
                .append("</p>");

        String attachmentName = "imagens-watermark-" + pending.packageId() + ".zip";
        return new EmailContent(subject, plain.toString(), html.toString(), attachmentName);
    }

    private void markAsNotified(long packageId, FlowSubmissionImagePackageStatus currentStatus) {
        FlowSubmissionImagePackageStatus nextStatus = currentStatus == FlowSubmissionImagePackageStatus.COMPLETED
                ? currentStatus
                : FlowSubmissionImagePackageStatus.COMPLETED;
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notified_at = ?, notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = NULL, status = ? WHERE id = ?",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                nextStatus.name(),
                packageId);
    }

    private void recordFailure(long packageId, String error) {
        String truncated = error == null ? "" : error;
        if (truncated.length() > 500) {
            truncated = truncated.substring(0, 500);
        }
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET notification_attempts = notification_attempts + 1, "
                        + "notification_last_attempt = ?, notification_last_error = ? WHERE id = ?",
                Timestamp.from(Instant.now()),
                truncated,
                packageId);
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

    private String resolveExtension(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            return ".jpg";
        }
        int dot = storedFileName.lastIndexOf('.');
        if (dot >= 0 && dot < storedFileName.length() - 1) {
            return storedFileName.substring(dot);
        }
        return ".jpg";
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
            Instant sampleUpdatedAt) {
    }

    private record WatermarkedAsset(String storedFileName, long assetId, int position) {
    }

    private record EmailContent(String subject, String plain, String html, String attachmentName) {
    }
}

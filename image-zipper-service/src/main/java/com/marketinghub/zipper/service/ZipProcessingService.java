package com.marketinghub.zipper.service;

import com.marketinghub.zipper.config.ZipperProperties;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ZipProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ZipProcessingService.class);
    private static final String CONTENT_TYPE_ZIP = "application/zip";

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final ZipperProperties properties;

    public ZipProcessingService(JdbcTemplate jdbcTemplate, StorageService storageService, ZipperProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.properties = properties;
    }

    @PostConstruct
    void logConfiguration() {
        log.info("Image zipper enabled: {}, batchSize={}, lockSeconds={}, maxAttempts={}", properties.isEnabled(), properties.getBatchSize(), properties.getLockSeconds(), properties.getMaxAttempts());
    }

    @Scheduled(initialDelayString = "${zipper.scheduler.initial-delay:20000}",
            fixedDelayString = "${zipper.scheduler.delay:60000}")
    public void pollAndProcess() {
        if (!properties.isEnabled()) {
            return;
        }
        List<PendingPackage> packages = findPendingPackages(Math.max(1, properties.getBatchSize()));
        for (PendingPackage pending : packages) {
            if (!lockPackage(pending.packageId())) {
                continue;
            }
            try {
                processPackage(pending.packageId());
            } catch (Exception ex) {
                log.error("Falha ao gerar zip para o pacote {}", pending.packageId(), ex);
                recordFailure(pending.packageId(), ex.getMessage());
            }
        }
    }

    private List<PendingPackage> findPendingPackages(int limit) {
        String sql = """
                SELECT
                    pack.id AS package_id,
                    pack.status,
                    pack.zip_attempts,
                    pack.zip_last_attempt,
                    pack.zip_last_error
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                WHERE pack.zip_object_key IS NULL
                  AND pack.status = 'COMPLETED'
                  AND exp.send_images_as_zip = TRUE
                  AND (pack.zip_last_attempt IS NULL
                       OR pack.zip_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                  AND pack.zip_attempts < ?
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

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPendingPackage(rs), properties.getLockSeconds(), properties.getMaxAttempts(), limit);
    }

    private PendingPackage mapPendingPackage(ResultSet rs) throws SQLException {
        return new PendingPackage(
                rs.getLong("package_id"),
                rs.getString("status"),
                rs.getInt("zip_attempts"),
                toInstant(rs.getTimestamp("zip_last_attempt")),
                rs.getString("zip_last_error")
        );
    }

    private boolean lockPackage(long packageId) {
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET zip_last_attempt = ?, zip_last_error = NULL "
                        + "WHERE id = ? AND zip_object_key IS NULL AND (zip_last_attempt IS NULL OR zip_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))",
                Timestamp.from(Instant.now()),
                packageId,
                properties.getLockSeconds());
        return updated > 0;
    }

    private void processPackage(long packageId) throws IOException {
        List<WatermarkedAsset> assets = fetchWatermarkedAssets(packageId);
        if (assets.isEmpty()) {
            throw new IllegalStateException("Nenhuma imagem com marca d'água encontrada para o pacote " + packageId);
        }

        byte[] zipBytes = buildZipArchive(assets);
        String objectKey = buildObjectKey(packageId);
        storageService.upload(objectKey, zipBytes, CONTENT_TYPE_ZIP);
        markSuccess(packageId, objectKey, zipBytes.length);
        log.info("Arquivo ZIP gerado para o pacote {} ({} bytes) em {}", packageId, zipBytes.length, objectKey);
    }

    private List<WatermarkedAsset> fetchWatermarkedAssets(long packageId) {
        String sql = """
                SELECT
                    COALESCE(opt_asset.external_id, opt_asset.url, wm_asset.external_id, wm_asset.url) AS stored_file_name,
                    item.position_index
                FROM flow_submission_image_item item
                JOIN flow_submission_image_watermark wm ON wm.item_id = item.id
                JOIN asset wm_asset ON wm_asset.id = wm.asset_id
                LEFT JOIN asset opt_asset ON opt_asset.id = wm.optimized_asset_id
                WHERE item.package_id = ?
                ORDER BY item.position_index ASC, item.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new WatermarkedAsset(
                rs.getString("stored_file_name"),
                rs.getInt("position_index")), packageId);
    }

    private byte[] buildZipArchive(List<WatermarkedAsset> assets) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int index = 1;
            for (WatermarkedAsset asset : assets) {
                if (!StringUtils.hasText(asset.storedFileName())) {
                    throw new IllegalStateException("Nome do arquivo do asset vazio");
                }
                byte[] bytes = storageService.download(asset.storedFileName());
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

    private void markSuccess(long packageId, String objectKey, long sizeBytes) {
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET zip_object_key = ?, zip_size_bytes = ?, zip_generated_at = ?, "
                        + "zip_last_error = NULL, zip_attempts = zip_attempts + 1, zip_last_attempt = ? WHERE id = ?",
                objectKey,
                sizeBytes,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                packageId);
    }

    private void recordFailure(long packageId, String errorMessage) {
        String truncated = errorMessage == null ? "" : errorMessage;
        if (truncated.length() > 500) {
            truncated = truncated.substring(0, 500);
        }
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET zip_attempts = zip_attempts + 1, zip_last_error = ?, zip_last_attempt = ? WHERE id = ?",
                truncated,
                Timestamp.from(Instant.now()),
                packageId);
    }

    private String buildObjectKey(long packageId) {
        String prefix = properties.getObjectPrefix();
        String normalized = prefix == null ? "" : prefix;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/lead-portal-package-" + packageId + ".zip";
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String resolveExtension(String storedFileName) {
        int dot = storedFileName.lastIndexOf('.');
        if (dot >= 0 && dot < storedFileName.length() - 1) {
            return storedFileName.substring(dot);
        }
        return ".jpg";
    }

    private record PendingPackage(long packageId, String status, int attempts, Instant lastAttempt, String lastError) {
    }

    private record WatermarkedAsset(String storedFileName, int position) {
    }
}

package com.marketinghub.zipper.service;

import com.marketinghub.zipper.config.PremiumDeliveryProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PremiumDeliveryZipService {

    private static final Logger log = LoggerFactory.getLogger(PremiumDeliveryZipService.class);
    private static final String CONTENT_TYPE_ZIP = "application/zip";

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final PremiumDeliveryProperties properties;

    public PremiumDeliveryZipService(JdbcTemplate jdbcTemplate,
                                     StorageService storageService,
                                     PremiumDeliveryProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.properties = properties;
    }

    @Scheduled(initialDelayString = "${premium-delivery.zipper.scheduler.initial-delay:20000}",
            fixedDelayString = "${premium-delivery.zipper.scheduler.delay:60000}")
    public void pollAndProcessPremiumDeliveries() {
        if (!properties.isEnabled()) {
            return;
        }
        List<PendingDelivery> deliveries = findPendingDeliveries(Math.max(1, properties.getBatchSize()));
        for (PendingDelivery delivery : deliveries) {
            if (!lockDelivery(delivery.id())) {
                continue;
            }
            int currentAttempt = delivery.zipAttempts() + 1;
            try {
                processDelivery(delivery);
            } catch (Exception ex) {
                log.error("Falha ao gerar ZIP premium para o delivery {} (package={})",
                        delivery.id(), delivery.packageId(), ex);
                recordFailure(delivery.id(), currentAttempt, ex.getMessage());
            }
        }
    }

    private List<PendingDelivery> findPendingDeliveries(int limit) {
        String sql = """
                SELECT
                    pd.id,
                    pd.package_id,
                    pd.status,
                    pd.zip_attempts,
                    pd.zip_last_attempt,
                    pd.zip_last_error
                FROM lead_portal_premium_delivery pd
                WHERE pd.status IN ('PENDING_ZIP', 'ZIPPING')
                  AND (pd.zip_last_attempt IS NULL
                       OR pd.zip_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                  AND pd.zip_attempts < ?
                ORDER BY pd.updated_at ASC, pd.id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPending(rs),
                properties.getLockSeconds(), properties.getMaxAttempts(), limit);
    }

    private PendingDelivery mapPending(ResultSet rs) throws SQLException {
        return new PendingDelivery(
                rs.getLong("id"),
                rs.getLong("package_id"),
                PremiumDeliveryStatus.valueOf(rs.getString("status")),
                rs.getInt("zip_attempts"),
                toInstant(rs.getTimestamp("zip_last_attempt")),
                rs.getString("zip_last_error"));
    }

    private boolean lockDelivery(long deliveryId) {
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    zip_last_attempt = UTC_TIMESTAMP(),
                    zip_attempts = zip_attempts + 1,
                    zip_last_error = NULL
                WHERE id = ?
                  AND status IN (?, ?)
                  AND (zip_last_attempt IS NULL OR zip_last_attempt < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP()))
                """;
        int updated = jdbcTemplate.update(sql,
                PremiumDeliveryStatus.ZIPPING.name(),
                deliveryId,
                PremiumDeliveryStatus.PENDING_ZIP.name(),
                PremiumDeliveryStatus.ZIPPING.name(),
                properties.getLockSeconds());
        return updated > 0;
    }

    private void processDelivery(PendingDelivery delivery) throws IOException {
        List<OriginalAsset> assets = fetchOriginalAssets(delivery.packageId());
        if (assets.isEmpty()) {
            throw new IllegalStateException("Nenhum asset encontrado para o pacote " + delivery.packageId());
        }
        byte[] zipBytes = buildZipArchive(assets);
        String objectKey = buildObjectKey(delivery.packageId());
        storageService.upload(objectKey, zipBytes, CONTENT_TYPE_ZIP);
        String downloadUrl = storageService.buildPublicUrl(objectKey).orElse(null);
        markSuccess(delivery.id(), objectKey, downloadUrl, zipBytes.length);
        log.info("ZIP premium gerado para pacote {} (delivery={}, bytes={})",
                delivery.packageId(), delivery.id(), zipBytes.length);
    }

    private List<OriginalAsset> fetchOriginalAssets(long packageId) {
        String sql = """
                SELECT items.id, items.position_index, a.url AS object_key
                FROM flow_submission_image_item items
                JOIN asset a ON a.id = items.asset_id
                WHERE items.package_id = ?
                ORDER BY items.position_index ASC, items.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OriginalAsset(
                rs.getLong("id"),
                (Integer) rs.getObject("position_index"),
                rs.getString("object_key")), packageId);
    }

    private byte[] buildZipArchive(List<OriginalAsset> assets) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            int index = 1;
            for (OriginalAsset asset : assets) {
                if (!StringUtils.hasText(asset.objectKey())) {
                    continue;
                }
                byte[] bytes = storageService.download(asset.objectKey());
                if (bytes == null || bytes.length == 0) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(resolveFileName(asset, index));
                zos.putNextEntry(entry);
                zos.write(bytes);
                zos.closeEntry();
                index++;
            }
            if (index == 1) {
                throw new IllegalStateException("Nenhum asset válido com conteúdo foi encontrado");
            }
            zos.finish();
            return baos.toByteArray();
        }
    }

    private String resolveFileName(OriginalAsset asset, int index) {
        String key = asset.objectKey();
        if (StringUtils.hasText(key)) {
            int slash = key.lastIndexOf('/') + 1;
            if (slash > 0 && slash < key.length()) {
                return key.substring(slash);
            }
            return key;
        }
        return String.format(Locale.ROOT, "imagem-premium-%02d%s", index, ".jpg");
    }

    private void markSuccess(long deliveryId, String objectKey, String downloadUrl, long sizeBytes) {
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    zip_object_key = ?,
                    zip_download_url = ?,
                    zip_size_bytes = ?,
                    zip_generated_at = UTC_TIMESTAMP(),
                    zip_last_error = NULL
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                PremiumDeliveryStatus.ZIP_READY.name(),
                objectKey,
                downloadUrl,
                sizeBytes,
                deliveryId);
    }

    private void recordFailure(long deliveryId, int attempts, String errorMessage) {
        boolean exhausted = attempts >= properties.getMaxAttempts();
        String status = exhausted ? PremiumDeliveryStatus.FAILED.name() : PremiumDeliveryStatus.PENDING_ZIP.name();
        String message = truncate(errorMessage, 500);
        String sql = """
                UPDATE lead_portal_premium_delivery
                SET status = ?,
                    zip_last_error = ?,
                    zip_last_attempt = UTC_TIMESTAMP()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, status, message, deliveryId);
    }

    private String buildObjectKey(long packageId) {
        String prefix = properties.getObjectPrefix() == null ? "" : properties.getObjectPrefix().trim();
        String normalized = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        if (!normalized.isEmpty()) {
            normalized = normalized + "/";
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        return normalized + "lead-portal/package-" + packageId + "/premium-" + token + ".zip";
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

    private record PendingDelivery(long id,
                                   long packageId,
                                   PremiumDeliveryStatus status,
                                   int zipAttempts,
                                   Instant lastAttempt,
                                   String lastError) {
    }

    private record OriginalAsset(long id, Integer positionIndex, String objectKey) {
    }
}

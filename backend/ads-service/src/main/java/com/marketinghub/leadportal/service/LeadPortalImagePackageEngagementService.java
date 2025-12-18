package com.marketinghub.leadportal.service;

import com.marketinghub.storage.FileStorageService;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Tracks engagement events (opens and views) for Lead Portal image packages.
 */
@Service
public class LeadPortalImagePackageEngagementService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageEngagementService.class);

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;

    public LeadPortalImagePackageEngagementService(JdbcTemplate jdbcTemplate, FileStorageService fileStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public boolean markEmailOpened(long packageId, String submissionToken) {
        Optional<EngagementTarget> target = loadTarget(packageId);
        if (target.isEmpty() || !matchesSubmission(target.get(), submissionToken)) {
            return false;
        }
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET email_opened_at = COALESCE(email_opened_at, ?), updated_at = ? WHERE id = ?",
                now,
                now,
                packageId);
        if (updated > 0) {
            log.debug("Marked lead portal package {} as email opened", packageId);
            return true;
        }
        return false;
    }

    @Transactional
    public Optional<String> markImagesViewed(long packageId, String submissionToken) {
        Optional<EngagementTarget> target = loadTarget(packageId);
        if (target.isEmpty() || !matchesSubmission(target.get(), submissionToken)) {
            return Optional.empty();
        }
        EngagementTarget engagementTarget = target.get();
        if (!StringUtils.hasText(engagementTarget.zipObjectKey())) {
            return Optional.empty();
        }

        Optional<String> downloadUrl = fileStorageService.resolvePublicUrl(engagementTarget.zipObjectKey());
        if (downloadUrl.isEmpty()) {
            return Optional.empty();
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET images_viewed_at = COALESCE(images_viewed_at, ?), "
                        + "email_opened_at = COALESCE(email_opened_at, ?), updated_at = ? WHERE id = ?",
                now,
                now,
                now,
                packageId);
        log.debug("Marked lead portal package {} as images viewed", packageId);
        return downloadUrl;
    }

    private Optional<EngagementTarget> loadTarget(long packageId) {
        try {
            return jdbcTemplate.query(
                            "SELECT submission_id, zip_object_key FROM flow_submission_image_package WHERE id = ?",
                            (rs, rowNum) -> mapTarget(rs),
                            packageId)
                    .stream()
                    .findFirst();
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private EngagementTarget mapTarget(ResultSet rs) throws SQLException {
        String submissionId = readSubmissionId(rs.getObject("submission_id"));
        String zipObjectKey = rs.getString("zip_object_key");
        return new EngagementTarget(submissionId, zipObjectKey);
    }

    private boolean matchesSubmission(EngagementTarget target, String submissionToken) {
        if (!StringUtils.hasText(submissionToken) || !StringUtils.hasText(target.submissionId())) {
            return true;
        }
        return submissionToken.trim().equalsIgnoreCase(target.submissionId());
    }

    private String readSubmissionId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof byte[] bytes) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                UUID submissionId = new UUID(buffer.getLong(), buffer.getLong());
                return submissionId.toString();
            } catch (Exception ex) {
                log.warn("Falha ao converter submission_id binário para UUID", ex);
                return null;
            }
        }
        String value = raw.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    private record EngagementTarget(String submissionId, String zipObjectKey) {
    }
}

package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Persists and retrieves the timeline of status changes for image packages generated in the Lead Portal.
 */
@Service
public class LeadPortalImagePackageStatusHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageStatusHistoryService.class);

    private static final String INSERT_SQL = "INSERT INTO flow_submission_image_package_status_history "
            + "(package_id, status, failure_reason, created_at) VALUES (?, ?, ?, ?)";
    private static final String SELECT_SQL = """
            SELECT status, failure_reason, created_at
            FROM flow_submission_image_package_status_history
            WHERE package_id = ?
            ORDER BY created_at ASC, id ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public LeadPortalImagePackageStatusHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordStatusChange(long packageId, FlowSubmissionImagePackageStatus status, String reason) {
        if (status == null) {
            return;
        }
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(INSERT_SQL, packageId, status.name(), normalizedReason, now);
    }

    public List<StatusHistoryEntry> listHistory(long packageId) {
        return jdbcTemplate.query(SELECT_SQL, (rs, rowNum) -> mapRow(rs), packageId);
    }

    private StatusHistoryEntry mapRow(ResultSet rs) throws SQLException {
        FlowSubmissionImagePackageStatus status = parseStatus(rs.getString("status"));
        String failureReason = rs.getString("failure_reason");
        if (failureReason != null && failureReason.isBlank()) {
            failureReason = null;
        }
        Instant occurredAt = toInstant(rs.getTimestamp("created_at"));
        return new StatusHistoryEntry(status, failureReason, occurredAt);
    }

    private FlowSubmissionImagePackageStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown flow_submission_image_package status history value '{}'", value);
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record StatusHistoryEntry(
            FlowSubmissionImagePackageStatus status,
            String failureReason,
            Instant occurredAt) {
    }
}

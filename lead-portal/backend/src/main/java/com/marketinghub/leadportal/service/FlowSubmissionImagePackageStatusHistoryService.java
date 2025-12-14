package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowSubmissionImagePackageStatusHistoryService {

    private static final String INSERT_SQL = "INSERT INTO flow_submission_image_package_status_history "
            + "(package_id, status, failure_reason, created_at) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public FlowSubmissionImagePackageStatusHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordStatusChange(Long packageId, FlowSubmissionImagePackageEntity.Status status, String reason) {
        if (packageId == null || status == null) {
            return;
        }
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        jdbcTemplate.update(INSERT_SQL, packageId, status.name(), normalizedReason, Timestamp.from(Instant.now()));
    }
}

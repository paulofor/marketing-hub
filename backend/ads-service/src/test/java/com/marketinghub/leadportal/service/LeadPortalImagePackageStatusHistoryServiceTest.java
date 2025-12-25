package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LeadPortalImagePackageStatusHistoryServiceTest {

    private JdbcTemplate jdbcTemplate;
    private LeadPortalImagePackageStatusHistoryService statusHistoryService;

    @BeforeEach
    void setUp() {
        String databaseName = "lead-portal-status-history-" + UUID.randomUUID();
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=-1".formatted(databaseName), "sa", "");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.statusHistoryService = new LeadPortalImagePackageStatusHistoryService(jdbcTemplate);

        jdbcTemplate.execute(
                "CREATE TABLE flow_submission_image_package_status_history ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "package_id BIGINT,"
                        + "status VARCHAR(64),"
                        + "failure_reason VARCHAR(1024),"
                        + "created_at TIMESTAMP"
                        + ")");
    }

    @Test
    void listHistoryReturnsChronologicalEntries() {
        long packageId = 99L;
        Instant receivedAt = Instant.parse("2024-05-10T10:15:30Z");
        Instant processingAt = receivedAt.plusSeconds(45);
        Instant failedAt = receivedAt.plusSeconds(90);

        statusHistoryService.recordStatusChange(
                packageId, FlowSubmissionImagePackageStatus.PROCESSING, null, processingAt);
        statusHistoryService.recordStatusChange(
                packageId, FlowSubmissionImagePackageStatus.RECEIVED, null, receivedAt);
        statusHistoryService.recordStatusChange(
                packageId, FlowSubmissionImagePackageStatus.FAILED, "boom", failedAt);

        List<LeadPortalImagePackageStatusHistoryService.StatusHistoryEntry> history =
                statusHistoryService.listHistory(packageId);

        assertThat(history)
                .extracting(LeadPortalImagePackageStatusHistoryService.StatusHistoryEntry::status)
                .containsExactly(
                        FlowSubmissionImagePackageStatus.RECEIVED,
                        FlowSubmissionImagePackageStatus.PROCESSING,
                        FlowSubmissionImagePackageStatus.FAILED);
        assertThat(history)
                .extracting(LeadPortalImagePackageStatusHistoryService.StatusHistoryEntry::occurredAt)
                .containsExactly(receivedAt, processingAt, failedAt);
    }
}

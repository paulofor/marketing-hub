package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.storage.FileStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LeadPortalImagePackageEngagementServiceTest {

    private JdbcTemplate jdbcTemplate;
    private LeadPortalImagePackageStatusHistoryService statusHistoryService;
    private FileStorageService fileStorageService;
    private LeadPortalImagePackageEngagementService engagementService;

    @BeforeEach
    void setUp() {
        String databaseName = "lead-portal-engagement-" + UUID.randomUUID();
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=-1".formatted(databaseName), "sa", "");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.statusHistoryService = new LeadPortalImagePackageStatusHistoryService(jdbcTemplate);
        this.fileStorageService = mock(FileStorageService.class);
        this.engagementService = new LeadPortalImagePackageEngagementService(
                jdbcTemplate, fileStorageService, statusHistoryService);

        jdbcTemplate.execute(
                "CREATE TABLE flow_submission_image_package ("
                        + "id BIGINT PRIMARY KEY,"
                        + "submission_id VARCHAR(64),"
                        + "zip_object_key VARCHAR(255),"
                        + "email_opened_at TIMESTAMP NULL,"
                        + "images_viewed_at TIMESTAMP NULL,"
                        + "updated_at TIMESTAMP NULL"
                        + ")");
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
    void markEmailOpenedRecordsHistoryEntry() {
        String submissionId = "123e4567-e89b-12d3-a456-426614174000";
        jdbcTemplate.update(
                "INSERT INTO flow_submission_image_package (id, submission_id) VALUES (?, ?)", 10L, submissionId);

        boolean updated = engagementService.markEmailOpened(10L, submissionId);

        assertThat(updated).isTrue();
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM flow_submission_image_package_status_history WHERE package_id = ? ORDER BY id",
                String.class,
                10L);
        assertThat(statuses)
                .containsExactly(FlowSubmissionImagePackageStatus.SAMPLE_EMAIL_OPENED.name());
    }

    @Test
    void markImagesViewedRecordsViewAndOpenHistory() {
        String submissionId = "bc3e3a8c-1c3a-4aa1-a8f3-1f8b551c9c33";
        jdbcTemplate.update(
                "INSERT INTO flow_submission_image_package (id, submission_id, zip_object_key) VALUES (?, ?, ?)",
                20L,
                submissionId,
                "sample.zip");
        when(fileStorageService.resolvePublicUrl("sample.zip"))
                .thenReturn(Optional.of("https://example.com/sample.zip"));

        Optional<String> downloadUrl = engagementService.markImagesViewed(20L, submissionId);

        assertThat(downloadUrl).contains("https://example.com/sample.zip");
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM flow_submission_image_package_status_history WHERE package_id = ? ORDER BY id",
                String.class,
                20L);
        assertThat(statuses)
                .containsExactly(
                        FlowSubmissionImagePackageStatus.SAMPLE_EMAIL_OPENED.name(),
                        FlowSubmissionImagePackageStatus.SAMPLE_IMAGES_VIEWED.name());
        List<Instant> occurrences = jdbcTemplate.query(
                "SELECT created_at FROM flow_submission_image_package_status_history WHERE package_id = ? ORDER BY id",
                (rs, rowNum) -> rs.getTimestamp("created_at").toInstant(),
                20L);
        assertThat(occurrences).hasSize(2);
    }
}

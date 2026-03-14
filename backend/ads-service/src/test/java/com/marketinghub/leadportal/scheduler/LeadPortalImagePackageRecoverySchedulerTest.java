package com.marketinghub.leadportal.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.config.LeadPortalProcessingGuardProperties;
import com.marketinghub.leadportal.service.LeadPortalImagePackageStatusHistoryService;
import com.marketinghub.leadportal.service.LeadPortalImagePackageStatusHistoryService.StatusHistoryEntry;
import com.marketinghub.leadportal.service.LeadPortalImagePackageWorkerService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class LeadPortalImagePackageRecoverySchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LeadPortalImagePackageWorkerService workerService;

    @Mock
    private LeadPortalImagePackageStatusHistoryService historyService;

    private LeadPortalProcessingGuardProperties properties;

    private LeadPortalImagePackageRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new LeadPortalProcessingGuardProperties();
        properties.setEnabled(true);
        properties.setTimeout(Duration.ofMinutes(30));
        properties.setBatchSize(10);
        properties.setMaxAttempts(2);

        scheduler = new LeadPortalImagePackageRecoveryScheduler(
                jdbcTemplate, workerService, historyService, properties);
    }

    @Test
    void requeuesPackagesStuckInProcessingWhenUnderLimit() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Long>>any(), any(), any()))
                .thenReturn(List.of(109L));

        when(historyService.listHistory(109L)).thenReturn(List.of(
                new StatusHistoryEntry(FlowSubmissionImagePackageStatus.RECENT, null, Instant.parse("2026-03-14T14:10:35Z")),
                new StatusHistoryEntry(FlowSubmissionImagePackageStatus.PROCESSING, null, Instant.parse("2026-03-14T14:10:50Z"))
        ));

        scheduler.recoverStuckPackages();

        verify(workerService).retry(eq(109L), contains("Reaberto automaticamente"));
        verify(workerService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void marksFailedWhenProcessingAttemptsExceeded() {
        properties.setMaxAttempts(1);

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Long>>any(), any(), any()))
                .thenReturn(List.of(77L));

        when(historyService.listHistory(77L)).thenReturn(List.of(
                new StatusHistoryEntry(FlowSubmissionImagePackageStatus.PROCESSING, null, Instant.parse("2026-03-10T10:00:00Z")),
                new StatusHistoryEntry(FlowSubmissionImagePackageStatus.PROCESSING, null, Instant.parse("2026-03-10T10:30:00Z"))
        ));

        scheduler.recoverStuckPackages();

        verify(workerService).markFailed(eq(77L), contains("FAILED"));
        verify(workerService, never()).retry(anyLong(), anyString());
    }

    @Test
    void doesNothingWhenGuardIsDisabled() {
        properties.setEnabled(false);

        scheduler.recoverStuckPackages();

        verifyNoInteractions(jdbcTemplate);
        verifyNoInteractions(workerService);
        verifyNoInteractions(historyService);
    }
}

package com.marketinghub.leadportal.scheduler;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.config.LeadPortalProcessingGuardProperties;
import com.marketinghub.leadportal.service.LeadPortalImagePackageStatusHistoryService;
import com.marketinghub.leadportal.service.LeadPortalImagePackageStatusHistoryService.StatusHistoryEntry;
import com.marketinghub.leadportal.service.LeadPortalImagePackageWorkerService;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Monitora pacotes que ficaram presos em PROCESSING e reabre ou falha automaticamente
 * para evitar que o pipeline fique parado indefinidamente.
 */
@Component
public class LeadPortalImagePackageRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageRecoveryScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final LeadPortalImagePackageWorkerService workerService;
    private final LeadPortalImagePackageStatusHistoryService historyService;
    private final LeadPortalProcessingGuardProperties properties;

    public LeadPortalImagePackageRecoveryScheduler(
            JdbcTemplate jdbcTemplate,
            LeadPortalImagePackageWorkerService workerService,
            LeadPortalImagePackageStatusHistoryService historyService,
            LeadPortalProcessingGuardProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.workerService = workerService;
        this.historyService = historyService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${lead-portal.worker.processing-guard.delay:PT5M}",
            initialDelayString = "${lead-portal.worker.processing-guard.initial-delay:PT1M}")
    public void recoverStuckPackages() {
        if (!properties.isEnabled()) {
            return;
        }

        Duration timeout = properties.getTimeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            log.warn("Lead portal processing guard disabled because timeout is invalid: {}", timeout);
            return;
        }

        long timeoutSeconds = Math.max(60, timeout.getSeconds());
        int batchSize = Math.max(1, properties.getBatchSize());
        int maxAttempts = Math.max(1, properties.getMaxAttempts());

        List<Long> stuckPackages = findStuckProcessingPackages(timeoutSeconds, batchSize);
        if (CollectionUtils.isEmpty(stuckPackages)) {
            return;
        }

        for (Long packageId : stuckPackages) {
            try {
                handlePackage(packageId, timeoutSeconds, maxAttempts);
            } catch (Exception ex) {
                log.error("Failed to recover lead portal image package {}", packageId, ex);
            }
        }
    }

    private List<Long> findStuckProcessingPackages(long timeoutSeconds, int limit) {
        String sql = """
                SELECT id
                FROM flow_submission_image_package
                WHERE status = 'PROCESSING'
                  AND updated_at < TIMESTAMPADD(SECOND, -?, UTC_TIMESTAMP())
                ORDER BY updated_at ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), timeoutSeconds, limit);
    }

    private void handlePackage(long packageId, long timeoutSeconds, int maxAttempts) {
        List<StatusHistoryEntry> history = historyService.listHistory(packageId);
        int processingAttempts = (int) history.stream()
                .filter(entry -> entry.status() == FlowSubmissionImagePackageStatus.PROCESSING)
                .count();

        if (processingAttempts >= maxAttempts) {
            String reason = buildFailureReason(timeoutSeconds, processingAttempts);
            log.warn(
                    "Marking lead portal package {} as FAILED after {}s in PROCESSING (processingAttempts={})",
                    packageId,
                    timeoutSeconds,
                    processingAttempts);
            workerService.markFailed(packageId, reason);
            return;
        }

        String reason = buildRetryReason(timeoutSeconds, processingAttempts, maxAttempts);
        log.warn(
                "Requeuing lead portal package {} after {}s stuck in PROCESSING (processingAttempts={})",
                packageId,
                timeoutSeconds,
                processingAttempts);
        workerService.retry(packageId, reason);
    }

    private String buildRetryReason(long timeoutSeconds, int attemptsSoFar, int maxAttempts) {
        String durationText = formatDuration(timeoutSeconds);
        int nextAttempt = attemptsSoFar + 1;
        return "Reaberto automaticamente após "
                + durationText
                + " em PROCESSING sem retorno do worker (tentativa "
                + nextAttempt
                + "/"
                + maxAttempts
                + ")";
    }

    private String buildFailureReason(long timeoutSeconds, int attempts) {
        String durationText = formatDuration(timeoutSeconds);
        return "Marcado como FAILED após "
                + durationText
                + " em PROCESSING sem retorno do worker (tentativas registradas: "
                + attempts
                + ")";
    }

    private String formatDuration(long timeoutSeconds) {
        long minutes = (long) Math.ceil(timeoutSeconds / 60.0);
        if (minutes < 120) {
            return minutes <= 1 ? "1 minuto" : minutes + " minutos";
        }
        long hours = (long) Math.ceil(minutes / 60.0);
        return hours <= 1 ? "1 hora" : hours + " horas";
    }
}

package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.RetrySalesVideoJobRequest;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduler que identifica jobs com falha elegíveis para reprocessamento automático.
 */
@Component
public class SalesVideoAutoRetryScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesVideoAutoRetryScheduler.class);
    private static final String AUTO_REQUESTED_BY = "auto@marketinghub.io";

    private final SalesVideoJobRepository jobRepository;
    private final SalesVideoJobService jobService;
    private final SalesVideoReprocessPolicy reprocessPolicy;
    private final boolean enabled;
    private final Duration retryDelay;
    private final int scanLimit;

    public SalesVideoAutoRetryScheduler(SalesVideoJobRepository jobRepository,
                                        SalesVideoJobService jobService,
                                        SalesVideoReprocessPolicy reprocessPolicy,
                                        @Value("${sales-video.reprocess.auto.enabled:true}") boolean enabled,
                                        @Value("${sales-video.reprocess.auto.retry-delay-minutes:15}") long retryDelayMinutes,
                                        @Value("${sales-video.reprocess.auto.scan-limit:20}") int scanLimit) {
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.reprocessPolicy = reprocessPolicy;
        this.enabled = enabled;
        this.retryDelay = Duration.ofMinutes(retryDelayMinutes);
        this.scanLimit = scanLimit;
    }

    @Scheduled(fixedDelayString = "${sales-video.reprocess.auto.scan-interval-ms:300000}")
    public void retryFailedJobs() {
        if (!enabled) {
            return;
        }
        Instant cutoff = Instant.now().minus(retryDelay);
        List<SalesVideoJob> candidates = jobRepository.findByStatusAndFinishedAtBefore(SalesVideoStatus.VIDEO_FAILED, cutoff);
        int executed = 0;
        for (SalesVideoJob job : candidates) {
            if (!reprocessPolicy.hasAttemptsRemaining(job)) {
                continue;
            }
            try {
                RetrySalesVideoJobRequest request = new RetrySalesVideoJobRequest();
                request.setRequestedBy(AUTO_REQUESTED_BY);
                request.setReason(SalesVideoRetryReason.AUTO_RECOVERY);
                request.setNotes("Retry automático após falha anterior");
                jobService.retry(job.getId(), request);
                executed++;
                if (executed >= scanLimit) {
                    break;
                }
            } catch (Exception ex) {
                LOGGER.warn("Falha ao reprocessar job {} automaticamente", job.getId(), ex);
            }
        }
        if (executed > 0) {
            LOGGER.info("Reprocessamento automático disparou {} novos jobs", executed);
        }
    }
}

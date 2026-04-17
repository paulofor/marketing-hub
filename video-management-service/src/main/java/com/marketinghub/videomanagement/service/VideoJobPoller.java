package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class VideoJobPoller {
    private final Logger log = LoggerFactory.getLogger(VideoJobPoller.class);
    private final VideoManagementProperties properties;
    private final BackendVideoClient backendClient;
    private final VideoJobDispatcher dispatcher;
    private final VideoJobObservabilityService observabilityService;

    public VideoJobPoller(VideoManagementProperties properties,
                          BackendVideoClient backendClient,
                          VideoJobDispatcher dispatcher,
                          VideoJobObservabilityService observabilityService) {
        this.properties = properties;
        this.backendClient = backendClient;
        this.dispatcher = dispatcher;
        this.observabilityService = observabilityService;
    }

    @Scheduled(initialDelay = 5000,
            fixedDelayString = "#{@videoManagementProperties.jobs.pollInterval.toMillis()}")
    public void pollJobs() {
        if (!properties.getJobs().isPollingEnabled()) {
            return;
        }
        try {
            List<SalesVideoJob> jobs = backendClient.fetchPendingJobs(properties.getJobs().getBatchSize());
            observabilityService.setBacklogRequested(jobs.size());
            if (!jobs.isEmpty()) {
                log.info("Encontrados {} jobs pendentes", jobs.size());
                jobs.forEach(dispatcher::dispatch);
            }
            if (properties.getJobs().isOrphanRecoveryEnabled()) {
                recoverOrphanJobs();
            }
        } catch (Exception ex) {
            log.error("Falha ao executar poll de jobs", ex);
        }
    }

    private void recoverOrphanJobs() {
        List<SalesVideoJob> processingJobs = backendClient.fetchJobsByStatus(
                SalesVideoStatus.VIDEO_PROCESSING, properties.getJobs().getBatchSize());
        observabilityService.setBacklogProcessing(processingJobs.size());
        if (processingJobs.isEmpty()) {
            return;
        }
        long staleBeforeMillis = System.currentTimeMillis() - properties.getJobs().getOrphanThreshold().toMillis();
        List<SalesVideoJob> orphanCandidates = processingJobs.stream()
                .filter(Objects::nonNull)
                .filter(job -> job.updatedAt() != null && job.updatedAt().toEpochMilli() <= staleBeforeMillis)
                .toList();
        if (orphanCandidates.isEmpty()) {
            return;
        }
        log.warn("Recuperação automática encontrou {} jobs órfãos/candidatos (threshold={}s)",
                orphanCandidates.size(), properties.getJobs().getOrphanThreshold().toSeconds());
        orphanCandidates.forEach(job -> {
            observabilityService.incrementOrphanRecovered(job.providerName());
            dispatcher.dispatch(job);
        });
    }
}

package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VideoJobPoller {
    private final Logger log = LoggerFactory.getLogger(VideoJobPoller.class);
    private final VideoManagementProperties properties;
    private final BackendVideoClient backendClient;
    private final VideoJobDispatcher dispatcher;

    public VideoJobPoller(VideoManagementProperties properties,
                          BackendVideoClient backendClient,
                          VideoJobDispatcher dispatcher) {
        this.properties = properties;
        this.backendClient = backendClient;
        this.dispatcher = dispatcher;
    }

    @Scheduled(initialDelay = 5000,
            fixedDelayString = "#{@videoManagementProperties.jobs.pollInterval.toMillis()}")
    public void pollJobs() {
        if (!properties.getJobs().isPollingEnabled()) {
            return;
        }
        try {
            List<SalesVideoJob> jobs = backendClient.fetchPendingJobs(properties.getJobs().getBatchSize());
            if (!jobs.isEmpty()) {
                log.info("Encontrados {} jobs pendentes", jobs.size());
                jobs.forEach(dispatcher::dispatch);
            }
        } catch (Exception ex) {
            log.error("Falha ao executar poll de jobs", ex);
        }
    }
}

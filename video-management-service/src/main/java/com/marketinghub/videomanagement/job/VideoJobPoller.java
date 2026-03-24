package com.marketinghub.videomanagement.job;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Task periódica responsável por consultar o backend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoJobPoller {
    private final VideoJobClient client;
    private final VideoJobProcessor processor;
    private final VideoManagementProperties properties;

    @Scheduled(fixedDelayString = "${video.jobs.poll-interval:PT30S}",
            initialDelayString = "5000")
    public void poll() {
        if (!properties.getJobs().isPollingEnabled()) {
            return;
        }
        List<VideoJobSummary> jobs = client.fetchPendingJobs();
        if (jobs.isEmpty()) {
            log.debug("[video-management] nenhum job pendente retornado pelo backend");
            return;
        }
        jobs.forEach(processor::process);
    }
}

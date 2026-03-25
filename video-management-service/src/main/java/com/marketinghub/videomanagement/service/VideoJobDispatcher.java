package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class VideoJobDispatcher {
    private final Logger log = LoggerFactory.getLogger(VideoJobDispatcher.class);
    private final VideoJobProcessor processor;
    private final ExecutorService executorService;
    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();

    public VideoJobDispatcher(VideoJobProcessor processor) {
        this.processor = processor;
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.executorService = Executors.newFixedThreadPool(threads);
    }

    public void dispatch(SalesVideoJob job) {
        if (job == null || job.id() == null) {
            return;
        }
        if (!runningJobs.add(job.id())) {
            log.debug("Job {} já está sendo processado", job.id());
            return;
        }
        executorService.submit(() -> {
            try {
                processor.process(job);
            } finally {
                runningJobs.remove(job.id());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}

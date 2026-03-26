package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Define limites de reprocessamento por tipo de job.
 */
@Component
public class SalesVideoReprocessPolicy {
    private final Map<SalesVideoJobType, Integer> maxAttempts;

    public SalesVideoReprocessPolicy(
            @Value("${sales-video.reprocess.script-max-attempts:2}") int scriptMax,
            @Value("${sales-video.reprocess.storyboard-max-attempts:2}") int storyboardMax,
            @Value("${sales-video.reprocess.render-max-attempts:3}") int renderMax,
            @Value("${sales-video.reprocess.publish-max-attempts:1}") int publishMax)
    {
        maxAttempts = new EnumMap<>(SalesVideoJobType.class);
        maxAttempts.put(SalesVideoJobType.SCRIPT, scriptMax);
        maxAttempts.put(SalesVideoJobType.STORYBOARD, storyboardMax);
        maxAttempts.put(SalesVideoJobType.RENDER, renderMax);
        maxAttempts.put(SalesVideoJobType.RETRY, renderMax);
        maxAttempts.put(SalesVideoJobType.PUBLISH, publishMax);
    }

    public void ensureRetryAllowed(SalesVideoJob job, Enum<?> reason) {
        int limit = resolveLimit(job.getJobType());
        if (job.getRetryAttempt() >= limit) {
            throw VideoModuleException.conflict(VideoModuleErrorCode.JOB_RETRY_POLICY_BLOCKED,
                    String.format("Limite de %d tentativas atingido para o job %d", limit, job.getId()));
        }
    }

    public boolean hasAttemptsRemaining(SalesVideoJob job) {
        return job.getRetryAttempt() < resolveLimit(job.getJobType());
    }

    private int resolveLimit(SalesVideoJobType jobType) {
        return maxAttempts.getOrDefault(jobType, 1);
    }
}

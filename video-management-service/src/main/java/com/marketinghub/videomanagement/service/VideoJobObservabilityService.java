package com.marketinghub.videomanagement.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VideoJobObservabilityService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger backlogRequested = new AtomicInteger(0);
    private final AtomicInteger backlogProcessing = new AtomicInteger(0);

    public VideoJobObservabilityService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("video_jobs_backlog", backlogRequested, AtomicInteger::get)
                .tag("status", "VIDEO_REQUESTED")
                .description("Quantidade de jobs em backlog no backend")
                .register(meterRegistry);
        Gauge.builder("video_jobs_backlog", backlogProcessing, AtomicInteger::get)
                .tag("status", "VIDEO_PROCESSING")
                .description("Quantidade de jobs em backlog no backend")
                .register(meterRegistry);
    }

    public void setBacklogRequested(int value) {
        backlogRequested.set(Math.max(value, 0));
    }

    public void setBacklogProcessing(int value) {
        backlogProcessing.set(Math.max(value, 0));
    }

    public void incrementJobsDispatched(String provider) {
        incrementCounter("video_jobs_dispatched_total", provider, null);
    }

    public void incrementJobsCompleted(String provider) {
        incrementCounter("video_jobs_completed_total", provider, null);
    }

    public void incrementJobsFailed(String provider, String failureCode) {
        incrementCounter("video_jobs_failed_total", provider, failureCode);
    }

    public void incrementAssetExpired(String provider) {
        incrementCounter("video_jobs_asset_expired_total", provider, null);
    }

    public void incrementClaimConflict(String provider) {
        incrementCounter("video_jobs_claim_conflict_total", provider, null);
    }

    public void incrementOrphanRecovered(String provider) {
        incrementCounter("video_jobs_orphan_recovery_total", provider, null);
    }

    public void incrementBackendRetry(String operation, Integer statusCode) {
        Counter.builder("video_backend_retry_total")
                .description("Retentativas técnicas de integração com backend")
                .tag("operation", normalize(operation))
                .tag("status", statusCode == null ? "unknown" : String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    public void recordRenderLatency(String provider, Duration latency) {
        if (latency == null || latency.isNegative()) {
            return;
        }
        Timer.builder("video_render_latency_seconds")
                .description("Latência total do render por job")
                .tag("provider", normalize(provider))
                .register(meterRegistry)
                .record(latency);
    }

    private void incrementCounter(String metric,
                                  String provider,
                                  String failureCode) {
        Counter.Builder builder = Counter.builder(metric)
                .description("Métrica operacional do módulo Avatar Sales Video")
                .tag("provider", normalize(provider));
        if (failureCode != null) {
            builder.tag("failure_code", normalize(failureCode));
        }
        builder.register(meterRegistry).increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}

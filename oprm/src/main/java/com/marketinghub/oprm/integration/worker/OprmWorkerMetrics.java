package com.marketinghub.oprm.integration.worker;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class OprmWorkerMetrics {
    private final Counter jobsClaimedCounter;
    private final Counter jobsSucceededCounter;
    private final Counter jobsFailedCounter;
    private final Counter artifactsPublishedCounter;
    private final Counter backendPublishFailuresCounter;
    private final Timer loopDurationTimer;
    private final MeterRegistry meterRegistry;
    private final AtomicLong jobsClaimed = new AtomicLong(0L);
    private final AtomicLong jobsSucceeded = new AtomicLong(0L);
    private final AtomicLong jobsFailed = new AtomicLong(0L);
    private final AtomicLong artifactsPublished = new AtomicLong(0L);
    private final AtomicLong backendPublishFailures = new AtomicLong(0L);

    public OprmWorkerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jobsClaimedCounter = Counter.builder("oprm.jobs.claimed").register(meterRegistry);
        this.jobsSucceededCounter = Counter.builder("oprm.jobs.succeeded").register(meterRegistry);
        this.jobsFailedCounter = Counter.builder("oprm.jobs.failed").register(meterRegistry);
        this.artifactsPublishedCounter = Counter.builder("oprm.artifacts.published").register(meterRegistry);
        this.backendPublishFailuresCounter = Counter.builder("oprm.backend.publish.failures").register(meterRegistry);
        this.loopDurationTimer = Timer.builder("oprm.loop.duration").register(meterRegistry);
    }

    public void incrementJobsClaimed() {
        jobsClaimedCounter.increment();
        jobsClaimed.incrementAndGet();
    }

    public void incrementJobsSucceeded() {
        jobsSucceededCounter.increment();
        jobsSucceeded.incrementAndGet();
    }

    public void incrementJobsFailed() {
        jobsFailedCounter.increment();
        jobsFailed.incrementAndGet();
    }

    public void incrementArtifactsPublished() {
        artifactsPublishedCounter.increment();
        artifactsPublished.incrementAndGet();
    }

    public void incrementBackendPublishFailures() {
        backendPublishFailuresCounter.increment();
        backendPublishFailures.incrementAndGet();
    }

    public void recordLoopDuration(long nanos) {
        loopDurationTimer.record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordPhaseDuration(String phase, long nanos) {
        Timer.builder("oprm.phase.duration")
                .tag("phase", phase)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    public Map<String, Object> countersSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();
        snapshot.put("jobsClaimed", jobsClaimed.get());
        snapshot.put("jobsSucceeded", jobsSucceeded.get());
        snapshot.put("jobsFailed", jobsFailed.get());
        snapshot.put("artifactsPublished", artifactsPublished.get());
        snapshot.put("backendPublishFailures", backendPublishFailures.get());
        return snapshot;
    }
}


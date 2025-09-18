package com.marketinghub.journey.execution.policy;

import com.marketinghub.journey.execution.JourneyExecutionProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes exponential backoff with jitter for retry attempts.
 */
@Component
public class RetryBackoffCalculator {
    private final JourneyExecutionProperties properties;

    public RetryBackoffCalculator(JourneyExecutionProperties properties) {
        this.properties = properties;
    }

    public Duration computeDelay(int attempt) {
        if (attempt < 1) {
            attempt = 1;
        }
        JourneyExecutionProperties.RetryProperties retry = properties.getRetry();
        long base = retry.getInitialBackoff().toMillis();
        long max = retry.getMaxBackoff().toMillis();
        long delay = base * (1L << Math.min(attempt - 1, 20));
        if (delay > max) {
            delay = max;
        }
        int jitterPercentage = retry.getJitterPercentage();
        if (jitterPercentage > 0) {
            double jitterFraction = jitterPercentage / 100.0;
            long jitter = Math.round(delay * ThreadLocalRandom.current().nextDouble(0.0, jitterFraction));
            delay = Math.min(max, delay + jitter);
        }
        return Duration.ofMillis(delay);
    }
}

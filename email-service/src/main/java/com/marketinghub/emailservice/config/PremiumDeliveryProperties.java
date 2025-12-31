package com.marketinghub.emailservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "premium-delivery.email")
public class PremiumDeliveryProperties {

    private boolean enabled = true;
    private int batchSize = 5;
    private int maxAttempts = 5;
    private int lockSeconds = 300;
    private Scheduler scheduler = new Scheduler();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLockSeconds() {
        return lockSeconds;
    }

    public void setLockSeconds(int lockSeconds) {
        this.lockSeconds = lockSeconds;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static class Scheduler {
        private long delay = 60000;
        private long initialDelay = 25000;

        public long getDelay() {
            return delay;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }
    }
}

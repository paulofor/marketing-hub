package com.marketinghub.mds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mds")
public class MdsProperties {
    private boolean loopEnabled = true;
    private int pollLimit = 5;
    private Backend backend = new Backend();
    private Search search = new Search();

    public boolean isLoopEnabled() {
        return loopEnabled;
    }

    public void setLoopEnabled(boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
    }

    public int getPollLimit() {
        return pollLimit;
    }

    public void setPollLimit(int pollLimit) {
        this.pollLimit = pollLimit;
    }

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public static class Backend {
        private String baseUrl = "http://localhost:8080";
        private String workerId = "mds-worker-local";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getWorkerId() {
            return workerId;
        }

        public void setWorkerId(String workerId) {
            this.workerId = workerId;
        }
    }

    public static class Search {
        private int timeoutMs = 5000;
        private int retryMaxAttempts = 2;
        private int retryBackoffMs = 250;

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getRetryMaxAttempts() {
            return retryMaxAttempts;
        }

        public void setRetryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
        }

        public int getRetryBackoffMs() {
            return retryBackoffMs;
        }

        public void setRetryBackoffMs(int retryBackoffMs) {
            this.retryBackoffMs = retryBackoffMs;
        }
    }
}

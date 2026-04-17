package com.marketinghub.mds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mds")
public class MdsProperties {
    private boolean loopEnabled = true;
    private int pollLimit = 5;
    private Backend backend = new Backend();

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
}

package com.marketinghub.worker.learning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurações gerais do pipeline de aprendizados de experimento.
 */
@Component
@ConfigurationProperties(prefix = "experiment.learning")
public class ExperimentLearningProperties {
    private boolean enabled = true;
    private int maxRequestsPerRun = 5;
    private String model;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRequestsPerRun() {
        return maxRequestsPerRun;
    }

    public void setMaxRequestsPerRun(int maxRequestsPerRun) {
        this.maxRequestsPerRun = maxRequestsPerRun;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

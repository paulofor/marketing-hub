package com.marketinghub.worker.report;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurações gerais utilizadas pelo pipeline de geração de relatórios de experimento.
 */
@Component
@ConfigurationProperties(prefix = "experiment.report")
public class ExperimentReportProperties {

    /** Habilita ou desabilita completamente o processamento automático. */
    private boolean enabled = true;

    /** Quantidade máxima de solicitações processadas por execução do scheduler. */
    private int maxRequestsPerRun = 5;

    /** Quantidade máxima de criativos renderizados no material final. */
    private int maxCreatives = 6;

    /** Prefixo usado para organizar os arquivos no bucket compartilhado. */
    private String storagePrefix = "reports";

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

    public int getMaxCreatives() {
        return maxCreatives;
    }

    public void setMaxCreatives(int maxCreatives) {
        this.maxCreatives = maxCreatives;
    }

    public String getStoragePrefix() {
        return storagePrefix;
    }

    public void setStoragePrefix(String storagePrefix) {
        this.storagePrefix = storagePrefix;
    }
}

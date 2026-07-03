package com.marketinghub.productaiworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Responsabilidade: parametrizar endpoints e limites operacionais do Product AI Worker. */
@ConfigurationProperties(prefix = "product-ai-worker")
public class ProductAiWorkerProperties {
    private boolean enabled = true;
    private String backendBaseUrl = "http://191.252.181.168";
    private int pendingLimit = 10;
    private String openAiBaseUrl = "https://api.openai.com/v1";
    private String openAiApiKey;

    /** Indica se o processamento periódico está ativo. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Define se o processamento periódico está ativo. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Retorna URL base do backend principal. */
    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    /** Define URL base do backend principal. */
    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Retorna limite de pendências processadas por ciclo. */
    public int getPendingLimit() {
        return pendingLimit;
    }

    /** Define limite de pendências processadas por ciclo. */
    public void setPendingLimit(int pendingLimit) {
        this.pendingLimit = pendingLimit;
    }

    /** Retorna URL base da OpenAI. */
    public String getOpenAiBaseUrl() {
        return openAiBaseUrl;
    }

    /** Define URL base da OpenAI. */
    public void setOpenAiBaseUrl(String openAiBaseUrl) {
        this.openAiBaseUrl = openAiBaseUrl;
    }

    /** Retorna chave OpenAI usada pelo worker. */
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    /** Define chave OpenAI usada pelo worker. */
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }
}

package com.marketinghub.scientificresearch.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Centraliza as configurações operacionais do worker de pesquisa científica.
 */
@Validated
@ConfigurationProperties(prefix = "scientific-research")
public class ScientificResearchProperties {

    private boolean enabled = true;

    @NotBlank
    private String backendBaseUrl = "http://191.252.181.168";

    @Min(1)
    @Max(20)
    private int pendingLimit = 5;

    @NotNull
    private Duration requestTimeout = Duration.ofSeconds(20);

    @NotBlank
    private String openAiBaseUrl = "https://api.openai.com/v1";

    private String openAiApiKey = "";

    @NotBlank
    private String openAiModel = "gpt-4.1";

    @NotBlank
    private String pubmedBaseUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";

    @NotBlank
    private String crossrefBaseUrl = "https://api.crossref.org";

    /**
     * Informa se o polling operacional está ativo.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Define se o polling operacional está ativo.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Retorna a URL base do backend principal.
     */
    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    /**
     * Define a URL base do backend principal.
     */
    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    /**
     * Retorna o limite de execuções buscadas por ciclo.
     */
    public int getPendingLimit() {
        return pendingLimit;
    }

    /**
     * Define o limite de execuções buscadas por ciclo.
     */
    public void setPendingLimit(int pendingLimit) {
        this.pendingLimit = pendingLimit;
    }

    /**
     * Retorna o timeout das chamadas HTTP.
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Define o timeout das chamadas HTTP.
     */
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Retorna a URL base da API da OpenAI.
     */
    public String getOpenAiBaseUrl() {
        return openAiBaseUrl;
    }

    /**
     * Define a URL base da API da OpenAI.
     */
    public void setOpenAiBaseUrl(String openAiBaseUrl) {
        this.openAiBaseUrl = openAiBaseUrl;
    }

    /**
     * Retorna a chave de API da OpenAI.
     */
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    /**
     * Define a chave de API da OpenAI.
     */
    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }

    /**
     * Retorna o modelo usado na síntese científica.
     */
    public String getOpenAiModel() {
        return openAiModel;
    }

    /**
     * Define o modelo usado na síntese científica.
     */
    public void setOpenAiModel(String openAiModel) {
        this.openAiModel = openAiModel;
    }

    /**
     * Retorna a URL base da API do PubMed.
     */
    public String getPubmedBaseUrl() {
        return pubmedBaseUrl;
    }

    /**
     * Define a URL base da API do PubMed.
     */
    public void setPubmedBaseUrl(String pubmedBaseUrl) {
        this.pubmedBaseUrl = pubmedBaseUrl;
    }

    /**
     * Retorna a URL base da API da Crossref.
     */
    public String getCrossrefBaseUrl() {
        return crossrefBaseUrl;
    }

    /**
     * Define a URL base da API da Crossref.
     */
    public void setCrossrefBaseUrl(String crossrefBaseUrl) {
        this.crossrefBaseUrl = crossrefBaseUrl;
    }
}

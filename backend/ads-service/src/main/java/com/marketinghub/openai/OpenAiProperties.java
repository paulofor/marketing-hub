package com.marketinghub.openai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Responsabilidade: concentrar propriedades de autenticação, timeouts e fontes oficiais da integração OpenAI. */
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;
    private String apiKeyFile = "/root/infra/openai-token/openai_api_key";
    private String pricingUrl = "https://platform.openai.com/docs/pricing";
    private String baseUrl = "https://api.openai.com/v1";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(90);
    private Duration batchPollInterval = Duration.ofMillis(500);
    private Duration batchTimeout = Duration.ofMinutes(2);
    private String batchCompletionWindow = "24h";

    /** Retorna o token OpenAI configurado diretamente por propriedade ou variável de ambiente. */
    public String getApiKey() {
        return apiKey;
    }

    /** Define o token OpenAI configurado diretamente por propriedade ou variável de ambiente. */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Retorna o caminho do arquivo seguro usado como fallback para carregar o token OpenAI. */
    public String getApiKeyFile() {
        return apiKeyFile;
    }

    /** Define o caminho do arquivo seguro usado como fallback para carregar o token OpenAI. */
    public void setApiKeyFile(String apiKeyFile) {
        this.apiKeyFile = apiKeyFile;
    }

    /** Retorna a URL oficial consultada para sincronizar preços dos modelos OpenAI. */
    public String getPricingUrl() {
        return pricingUrl;
    }

    /** Define a URL oficial consultada para sincronizar preços dos modelos OpenAI. */
    public void setPricingUrl(String pricingUrl) {
        this.pricingUrl = pricingUrl;
    }

    /** Retorna a URL base da API OpenAI usada nas chamadas autenticadas. */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** Define a URL base da API OpenAI usada nas chamadas autenticadas. */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Retorna o timeout de conexão da integração OpenAI. */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /** Define o timeout de conexão da integração OpenAI. */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /** Retorna o timeout total de leitura/resposta da integração OpenAI. */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /** Define o timeout total de leitura/resposta da integração OpenAI. */
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /** Retorna o intervalo de consulta para acompanhamento de batches OpenAI. */
    public Duration getBatchPollInterval() {
        return batchPollInterval;
    }

    /** Define o intervalo de consulta para acompanhamento de batches OpenAI. */
    public void setBatchPollInterval(Duration batchPollInterval) {
        this.batchPollInterval = batchPollInterval;
    }

    /** Retorna o timeout máximo para conclusão de batches OpenAI. */
    public Duration getBatchTimeout() {
        return batchTimeout;
    }

    /** Define o timeout máximo para conclusão de batches OpenAI. */
    public void setBatchTimeout(Duration batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    /** Retorna a janela de conclusão declarada ao criar batches OpenAI. */
    public String getBatchCompletionWindow() {
        return batchCompletionWindow;
    }

    /** Define a janela de conclusão declarada ao criar batches OpenAI. */
    public void setBatchCompletionWindow(String batchCompletionWindow) {
        this.batchCompletionWindow = batchCompletionWindow;
    }

    /** Indica se existe token direto ou arquivo seguro realmente disponível para chamadas autenticadas à OpenAI. */
    public boolean isEnabled() {
        return StringUtils.hasText(apiKey) || (StringUtils.hasText(apiKeyFile) && Files.isRegularFile(Path.of(apiKeyFile)));
    }
}

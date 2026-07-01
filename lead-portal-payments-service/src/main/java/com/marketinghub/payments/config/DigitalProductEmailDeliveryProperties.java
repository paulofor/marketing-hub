package com.marketinghub.payments.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configura a integração de email pós-compra para produtos digitais validados por checkout direto.
 */
@ConfigurationProperties(prefix = "digital-product.delivery.email")
public class DigitalProductEmailDeliveryProperties {

    private boolean enabled = true;
    private String emailServiceBaseUrl = "http://191.252.120.96:8086";
    private String sendPath = "/api/v1/product-deliveries/send";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(20);
    private String experiment51Reference = "marketinghub-experiment-51";
    private String experiment51ProductName = "Mapa de Recorrência 7D";
    private String experiment51DeliveryPageUrl = "https://pagamentopalf.site/obrigado-exp51-mapa-recorrencia-7d.html";
    private String experiment51DownloadUrl =
            "https://pagamentopalf.site/downloads/mapa-recorrencia-7d-entrega-v0-7xQ9mR4pL2.zip";

    /** Indica se o envio automático está ativo. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Define se o envio automático está ativo. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Retorna a URL base pública ou interna do email-service. */
    public String getEmailServiceBaseUrl() {
        return emailServiceBaseUrl;
    }

    /** Define a URL base pública ou interna do email-service. */
    public void setEmailServiceBaseUrl(String emailServiceBaseUrl) {
        this.emailServiceBaseUrl = emailServiceBaseUrl;
    }

    /** Retorna o caminho do endpoint de envio de entrega digital. */
    public String getSendPath() {
        return sendPath;
    }

    /** Define o caminho do endpoint de envio de entrega digital. */
    public void setSendPath(String sendPath) {
        this.sendPath = sendPath;
    }

    /** Retorna o timeout de conexão com o email-service. */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /** Define o timeout de conexão com o email-service. */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /** Retorna o timeout de leitura do email-service. */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /** Define o timeout de leitura do email-service. */
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /** Retorna a referência Mercado Pago do experimento 51. */
    public String getExperiment51Reference() {
        return experiment51Reference;
    }

    /** Define a referência Mercado Pago do experimento 51. */
    public void setExperiment51Reference(String experiment51Reference) {
        this.experiment51Reference = experiment51Reference;
    }

    /** Retorna o nome comercial do produto do experimento 51. */
    public String getExperiment51ProductName() {
        return experiment51ProductName;
    }

    /** Define o nome comercial do produto do experimento 51. */
    public void setExperiment51ProductName(String experiment51ProductName) {
        this.experiment51ProductName = experiment51ProductName;
    }

    /** Retorna a página de entrega do experimento 51. */
    public String getExperiment51DeliveryPageUrl() {
        return experiment51DeliveryPageUrl;
    }

    /** Define a página de entrega do experimento 51. */
    public void setExperiment51DeliveryPageUrl(String experiment51DeliveryPageUrl) {
        this.experiment51DeliveryPageUrl = experiment51DeliveryPageUrl;
    }

    /** Retorna a URL de download do produto do experimento 51. */
    public String getExperiment51DownloadUrl() {
        return experiment51DownloadUrl;
    }

    /** Define a URL de download do produto do experimento 51. */
    public void setExperiment51DownloadUrl(String experiment51DownloadUrl) {
        this.experiment51DownloadUrl = experiment51DownloadUrl;
    }
}

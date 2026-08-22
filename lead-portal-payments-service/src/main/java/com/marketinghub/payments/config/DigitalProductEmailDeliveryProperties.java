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
    private String experiment66Reference = "marketinghub-experiment-66";
    private String experiment66ProductName = "Método MUSA - Presença Elegante em 7 Dias";
    private String experiment66DeliveryPageUrl = "https://pagamentopalf.site/obrigado-exp66-metodo-musa.html";
    private String experiment66DownloadUrl =
            "https://pagamentopalf.site/downloads/experimento-66-entregaveis.zip";
    private String agendaCheiaReference = "agenda-cheia-nail-design";
    private String agendaCheiaProductName = "Agenda Cheia Nail Design";
    private String agendaCheiaDeliveryPageUrl = "https://pagamentopalf.site/agenda-cheia/obrigado.html";
    private String kitWhatsAppReference = "kit-whatsapp-pronto";
    private String kitWhatsAppProductName = "Kit WhatsApp Pronto";
    private String kitWhatsAppDeliveryPageUrl =
            "https://kit-whatsapp-pronto.digicomdigital.com.br";

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

    /** Retorna a referência Mercado Pago do experimento 66. */
    public String getExperiment66Reference() {
        return experiment66Reference;
    }

    /** Define a referência Mercado Pago do experimento 66. */
    public void setExperiment66Reference(String experiment66Reference) {
        this.experiment66Reference = experiment66Reference;
    }

    /** Retorna o nome comercial do produto do experimento 66. */
    public String getExperiment66ProductName() {
        return experiment66ProductName;
    }

    /** Define o nome comercial do produto do experimento 66. */
    public void setExperiment66ProductName(String experiment66ProductName) {
        this.experiment66ProductName = experiment66ProductName;
    }

    /** Retorna a página de entrega do experimento 66. */
    public String getExperiment66DeliveryPageUrl() {
        return experiment66DeliveryPageUrl;
    }

    /** Define a página de entrega do experimento 66. */
    public void setExperiment66DeliveryPageUrl(String experiment66DeliveryPageUrl) {
        this.experiment66DeliveryPageUrl = experiment66DeliveryPageUrl;
    }

    /** Retorna a URL de download do produto do experimento 66. */
    public String getExperiment66DownloadUrl() {
        return experiment66DownloadUrl;
    }

    /** Define a URL de download do produto do experimento 66. */
    public void setExperiment66DownloadUrl(String experiment66DownloadUrl) {
        this.experiment66DownloadUrl = experiment66DownloadUrl;
    }

    /** Retorna a referência usada pelo checkout do Agenda Cheia. */
    public String getAgendaCheiaReference() { return agendaCheiaReference; }

    /** Define a referência usada pelo checkout do Agenda Cheia. */
    public void setAgendaCheiaReference(String value) { agendaCheiaReference = value; }

    /** Retorna o nome comercial do Agenda Cheia. */
    public String getAgendaCheiaProductName() { return agendaCheiaProductName; }

    /** Define o nome comercial do Agenda Cheia. */
    public void setAgendaCheiaProductName(String value) { agendaCheiaProductName = value; }

    /** Retorna a página pública do briefing pós-compra. */
    public String getAgendaCheiaDeliveryPageUrl() { return agendaCheiaDeliveryPageUrl; }

    /** Define a página pública do briefing pós-compra. */
    public void setAgendaCheiaDeliveryPageUrl(String value) { agendaCheiaDeliveryPageUrl = value; }

    /** Retorna a referência comercial do Kit WhatsApp Pronto. */
    public String getKitWhatsAppReference() { return kitWhatsAppReference; }

    /** Define a referência comercial do Kit WhatsApp Pronto. */
    public void setKitWhatsAppReference(String value) { kitWhatsAppReference = value; }

    /** Retorna o nome público do Kit WhatsApp Pronto. */
    public String getKitWhatsAppProductName() { return kitWhatsAppProductName; }

    /** Define o nome público do Kit WhatsApp Pronto. */
    public void setKitWhatsAppProductName(String value) { kitWhatsAppProductName = value; }

    /** Retorna a área de acesso entregue após o pagamento. */
    public String getKitWhatsAppDeliveryPageUrl() { return kitWhatsAppDeliveryPageUrl; }

    /** Define a área de acesso entregue após o pagamento. */
    public void setKitWhatsAppDeliveryPageUrl(String value) { kitWhatsAppDeliveryPageUrl = value; }
}

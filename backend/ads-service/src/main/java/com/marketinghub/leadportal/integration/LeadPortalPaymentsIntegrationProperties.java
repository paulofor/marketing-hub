package com.marketinghub.leadportal.integration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Mantém a configuração versionada da integração oficial de pagamentos do Lead Portal. */
@Component
@ConfigurationProperties(prefix = "integrations.lead-portal-payments")
public class LeadPortalPaymentsIntegrationProperties {

  private boolean enabled = true;
  private String baseUrl = "https://pagamentopalf.site";
  private String authToken;
  private Duration connectTimeout = Duration.ofSeconds(2);
  private Duration readTimeout = Duration.ofSeconds(20);

  /** Informa se a integração de pagamentos está habilitada. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Habilita ou desabilita a integração de pagamentos. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Retorna a URL-base do serviço oficial de pagamentos. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** Define a URL-base do serviço oficial de pagamentos. */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** Retorna o token interno usado na comunicação entre serviços. */
  public String getAuthToken() {
    return authToken;
  }

  /** Define o token interno usado na comunicação entre serviços. */
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /** Retorna o limite de tempo para estabelecer a conexão. */
  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  /** Define o limite de tempo para estabelecer a conexão. */
  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  /** Retorna a janela máxima para receber a resposta do serviço de pagamentos. */
  public Duration getReadTimeout() {
    return readTimeout;
  }

  /** Define a janela máxima para receber a resposta do serviço de pagamentos. */
  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }
}

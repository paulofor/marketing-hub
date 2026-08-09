package com.marketinghub.metaadapproverworker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Responsabilidade: reunir limites e endereços operacionais do módulo aprovador. */
@ConfigurationProperties("meta-ad-approver")
public class MetaAdApproverProperties {
  private String backendUrl = "http://backend:8000";
  private String marketingHubUrl = "http://backend:8000";
  private String repositoryPath = "/workspace/marketing-hub";
  private String codexCommand = "codex";
  private String model = "gpt-5.6-sol";
  private String reasoningEffort = "high";
  private Duration codexTimeout = Duration.ofMinutes(40);
  private int pendingLimit = 3;

  /** Retorna a URL do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Define a URL do backend. */
  public void setBackendUrl(String value) {
    backendUrl = value;
  }

  /** Retorna a URL consultada pelo MCP. */
  public String getMarketingHubUrl() {
    return marketingHubUrl;
  }

  /** Define a URL consultada pelo MCP. */
  public void setMarketingHubUrl(String value) {
    marketingHubUrl = value;
  }

  /** Retorna o repositório montado somente para leitura. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Define o repositório montado. */
  public void setRepositoryPath(String value) {
    repositoryPath = value;
  }

  /** Retorna o comando Codex. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Define o comando Codex. */
  public void setCodexCommand(String value) {
    codexCommand = value;
  }

  /** Retorna o modelo Codex. */
  public String getModel() {
    return model;
  }

  /** Define o modelo Codex. */
  public void setModel(String value) {
    model = value;
  }

  /** Retorna o esforço de raciocínio. */
  public String getReasoningEffort() {
    return reasoningEffort;
  }

  /** Define o esforço de raciocínio. */
  public void setReasoningEffort(String value) {
    reasoningEffort = value;
  }

  /** Retorna o timeout da sandbox. */
  public Duration getCodexTimeout() {
    return codexTimeout;
  }

  /** Define o timeout da sandbox. */
  public void setCodexTimeout(Duration value) {
    codexTimeout = value;
  }

  /** Retorna o limite do lote. */
  public int getPendingLimit() {
    return pendingLimit;
  }

  /** Define o limite do lote. */
  public void setPendingLimit(int value) {
    pendingLimit = Math.max(1, value);
  }
}

package com.marketinghub.communicationagentworker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: centralizar a configuração operacional do executor de Íris. */
@Component
@ConfigurationProperties(prefix = "communication-agent")
public class CommunicationAgentProperties {
  private String backendUrl;
  private String repositoryPath;
  private String codexCommand;
  private String model;
  private String reasoningEffort = "high";
  private String serviceTier = "default";
  private String serviceTierExceptionReason =
      "O catálogo do Codex OAuth não anuncia Flex para os modelos disponíveis ao harness.";
  private Duration codexTimeout = Duration.ofMinutes(40);

  /** Retorna a URL canônica do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Configura a URL canônica do backend. */
  public void setBackendUrl(String backendUrl) {
    this.backendUrl = backendUrl;
  }

  /** Retorna o caminho somente leitura do repositório. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Configura o caminho somente leitura do repositório. */
  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  /** Retorna o comando Codex autenticado. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Configura o comando Codex autenticado. */
  public void setCodexCommand(String codexCommand) {
    this.codexCommand = codexCommand;
  }

  /** Retorna o modelo configurado. */
  public String getModel() {
    return model;
  }

  /** Configura o modelo utilizado por Íris. */
  public void setModel(String model) {
    this.model = model;
  }

  /** Retorna o esforço de raciocínio auditado. */
  public String getReasoningEffort() {
    return reasoningEffort;
  }

  /** Configura o esforço de raciocínio auditado. */
  public void setReasoningEffort(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }

  /** Exige o esforço explícito usado tanto no comando quanto na auditoria de Íris. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || reasoningEffort.isBlank()) {
      throw new IllegalStateException(
          "COMMUNICATION_AGENT_REASONING_EFFORT é obrigatório para auditar Íris.");
    }
    return reasoningEffort.trim();
  }

  /** Retorna o tier realmente solicitado ao runtime Codex. */
  public String getServiceTier() {
    return serviceTier;
  }

  /** Configura o tier somente quando ele estiver anunciado pelo catálogo do Codex. */
  public void setServiceTier(String serviceTier) {
    this.serviceTier = serviceTier;
  }

  /** Retorna a justificativa auditável para não usar Flex neste runtime. */
  public String getServiceTierExceptionReason() {
    return serviceTierExceptionReason;
  }

  /** Configura a justificativa auditável da exceção de tier. */
  public void setServiceTierExceptionReason(String serviceTierExceptionReason) {
    this.serviceTierExceptionReason = serviceTierExceptionReason;
  }

  /** Retorna o limite de duração de uma execução. */
  public Duration getCodexTimeout() {
    return codexTimeout;
  }

  /** Configura o limite de duração de uma execução. */
  public void setCodexTimeout(Duration codexTimeout) {
    this.codexTimeout = codexTimeout;
  }
}

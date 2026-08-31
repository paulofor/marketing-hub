package com.marketinghub.experimentstrategistworker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: concentrar configuracoes operacionais do Estrategista. */
@Component
@ConfigurationProperties(prefix = "experiment-strategist")
public class WorkerProperties {
  private String backendUrl;
  private String repositoryPath;
  private String codexCommand;
  private String model;
  private String reasoningEffort = "high";
  private Duration codexTimeout = Duration.ofMinutes(40);
  private String clarityApiTokenFile;

  /** Retorna a URL do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Configura a URL do backend. */
  public void setBackendUrl(String value) {
    backendUrl = value;
  }

  /** Retorna o repositorio somente leitura. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Configura o repositorio somente leitura. */
  public void setRepositoryPath(String value) {
    repositoryPath = value;
  }

  /** Retorna o comando Codex. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Configura o comando Codex. */
  public void setCodexCommand(String value) {
    codexCommand = value;
  }

  /** Retorna o modelo configurado. */
  public String getModel() {
    return model;
  }

  /** Configura o modelo. */
  public void setModel(String value) {
    model = value;
  }

  /** Retorna o esforço de raciocínio auditado nas atividades de Atena. */
  public String getReasoningEffort() {
    return reasoningEffort;
  }

  /** Configura o esforço de raciocínio usado e persistido por Atena. */
  public void setReasoningEffort(String value) {
    reasoningEffort = value;
  }

  /** Exige esforço explícito antes de qualquer chamada de modelo. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || reasoningEffort.isBlank()) {
      throw new IllegalStateException(
          "EXPERIMENT_STRATEGIST_REASONING_EFFORT é obrigatório para auditar Atena.");
    }
    return reasoningEffort.trim();
  }

  /** Retorna o timeout do Codex. */
  public Duration getCodexTimeout() {
    return codexTimeout;
  }

  /** Configura o timeout do Codex. */
  public void setCodexTimeout(Duration value) {
    codexTimeout = value;
  }

  /** Retorna o arquivo secreto do token da API Data Export do Clarity. */
  public String getClarityApiTokenFile() {
    return clarityApiTokenFile;
  }

  /** Configura o arquivo secreto do token da API Data Export do Clarity. */
  public void setClarityApiTokenFile(String value) {
    clarityApiTokenFile = value;
  }
}

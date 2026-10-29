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
  private String model = "gpt-6-astra";
  private String reasoningEffort = "max";
  private Duration codexTimeout = Duration.ofMinutes(40);
  private String clarityApiTokenFile;
  private String bpmStateDirectory = "/var/lib/atena/bpm-market-strategy";

  /** Retorna o armazenamento persistente das entregas BPM ainda não confirmadas. */
  public String getBpmStateDirectory() {
    return bpmStateDirectory;
  }

  /** Configura armazenamento exclusivo desta fila, independente da sessão Codex. */
  public void setBpmStateDirectory(String value) {
    bpmStateDirectory = value;
  }

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

  /** Retorna max validado para chamadas e auditorias, recusando configuração inferior. */
  public String getReasoningEffort() {
    return requiredReasoningEffort();
  }

  /** Configura o esforço de raciocínio usado e persistido por Atena. */
  public void setReasoningEffort(String value) {
    reasoningEffort = value;
  }

  /** Exige max antes da chamada e mantém comando e auditoria com a mesma configuração. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || !"max".equals(reasoningEffort.trim())) {
      throw new IllegalStateException(
          "EXPERIMENT_STRATEGIST_REASONING_EFFORT deve ser max, obrigatório para auditar Atena.");
    }
    return "max";
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

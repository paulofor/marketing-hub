package com.marketinghub.growthoperatorworker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: centralizar configuracoes operacionais do worker. */
@Component
@ConfigurationProperties(prefix = "growth-operator")
public class WorkerProperties {
  private String backendUrl;
  private String repositoryPath;
  private String codexCommand;
  private String model;
  private String reasoningEffort = "high";
  private String marketingHubUrl;
  private Long commercialPlanId;
  private Duration codexTimeout = Duration.ofMinutes(40);

  /** Retorna a URL do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Configura a URL do backend. */
  public void setBackendUrl(String backendUrl) {
    this.backendUrl = backendUrl;
  }

  /** Retorna o caminho somente leitura do repositorio. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Configura o caminho do repositorio. */
  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  /** Retorna o comando Codex instalado na imagem. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Configura o comando Codex. */
  public void setCodexCommand(String codexCommand) {
    this.codexCommand = codexCommand;
  }

  /** Retorna o modelo opcional configurado. */
  public String getModel() {
    return model;
  }

  /** Configura o modelo opcional. */
  public void setModel(String model) {
    this.model = model;
  }

  /** Retorna o esforço de raciocínio aplicado ao diagnóstico. */
  public String getReasoningEffort() {
    return reasoningEffort;
  }

  /** Configura o esforço de raciocínio aplicado ao diagnóstico. */
  public void setReasoningEffort(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }

  /** Exige o esforço explícito compartilhado pelo comando e pela auditoria de Hermes. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || reasoningEffort.isBlank()) {
      throw new IllegalStateException(
          "GROWTH_OPERATOR_REASONING_EFFORT é obrigatório para auditar Hermes.");
    }
    return reasoningEffort.trim();
  }

  /** Retorna a URL publica usada pelo Codex para consultar o Marketing Hub. */
  public String getMarketingHubUrl() {
    return marketingHubUrl;
  }

  /** Configura a URL publica usada nas investigacoes do agente. */
  public void setMarketingHubUrl(String marketingHubUrl) {
    this.marketingHubUrl = marketingHubUrl;
  }

  /** Retorna o planejamento semanal acompanhado continuamente. */
  public Long getCommercialPlanId() {
    return commercialPlanId;
  }

  /** Configura o planejamento semanal acompanhado continuamente. */
  public void setCommercialPlanId(Long commercialPlanId) {
    this.commercialPlanId = commercialPlanId;
  }

  /** Retorna o tempo máximo permitido para uma execução do Codex. */
  public Duration getCodexTimeout() {
    return codexTimeout;
  }

  /** Configura o tempo máximo permitido para uma execução do Codex. */
  public void setCodexTimeout(Duration codexTimeout) {
    this.codexTimeout = codexTimeout;
  }
}

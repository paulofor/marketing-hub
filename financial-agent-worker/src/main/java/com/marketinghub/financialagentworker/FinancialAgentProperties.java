package com.marketinghub.financialagentworker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: centralizar configuracoes operacionais do Agente Financeiro. */
@Component
@ConfigurationProperties(prefix = "financial-agent")
public class FinancialAgentProperties {
  private String backendUrl;
  private String repositoryPath;
  private String codexCommand;
  private String model;
  private Long commercialPlanId;

  /** Retorna a URL oficial do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Configura a URL oficial do backend. */
  public void setBackendUrl(String backendUrl) {
    this.backendUrl = backendUrl;
  }

  /** Retorna o repositorio montado somente leitura. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Configura o repositorio montado somente leitura. */
  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  /** Retorna o comando Codex. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Configura o comando Codex. */
  public void setCodexCommand(String codexCommand) {
    this.codexCommand = codexCommand;
  }

  /** Retorna o modelo configurado. */
  public String getModel() {
    return model;
  }

  /** Configura o modelo do agente. */
  public void setModel(String model) {
    this.model = model;
  }

  /** Retorna o planejamento acompanhado diariamente. */
  public Long getCommercialPlanId() {
    return commercialPlanId;
  }

  /** Configura o planejamento acompanhado diariamente. */
  public void setCommercialPlanId(Long commercialPlanId) {
    this.commercialPlanId = commercialPlanId;
  }
}

package com.marketinghub.growthoperatorworker;

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
}

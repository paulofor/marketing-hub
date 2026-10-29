package com.marketinghub.landinggeneratoragent;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Responsabilidade: reunir limites e endereços operacionais do Agente Gerador de Landing. */
@ConfigurationProperties("landing-generator-agent")
public class LandingGeneratorAgentProperties {
  private String backendUrl = "http://backend:8000";
  private String marketingHubUrl = "http://backend:8000";
  private String repositoryPath = "/workspace/marketing-hub";
  private String mcpScriptPath = "/app/mcp/landing-generator.mjs";
  private String codexCommand = "codex";
  private String model = "gpt-6.1-sol";
  private String strategicModel = "gpt-6-astra";
  private String reasoningEffort = "max";
  private String buildReference = "local";
  private String bpmStateDirectory = System.getProperty("user.home") + "/.codex/dedalo-bpm";
  private Duration codexTimeout = Duration.ofMinutes(40);

  /** Retorna a URL do backend. */
  public String getBackendUrl() {
    return backendUrl;
  }

  /** Define a URL do backend. */
  public void setBackendUrl(String value) {
    backendUrl = value;
  }

  /** Retorna a URL permitida ao MCP. */
  public String getMarketingHubUrl() {
    return marketingHubUrl;
  }

  /** Define a URL permitida ao MCP. */
  public void setMarketingHubUrl(String value) {
    marketingHubUrl = value;
  }

  /** Retorna o repositório somente leitura. */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /** Define o repositório somente leitura. */
  public void setRepositoryPath(String value) {
    repositoryPath = value;
  }

  /** Retorna o script MCP instalado junto das dependências Node da imagem. */
  public String getMcpScriptPath() {
    return mcpScriptPath;
  }

  /** Define o script MCP executável sem cópia para diretório temporário. */
  public void setMcpScriptPath(String value) {
    mcpScriptPath = value;
  }

  /** Retorna o comando Codex. */
  public String getCodexCommand() {
    return codexCommand;
  }

  /** Define o comando Codex. */
  public void setCodexCommand(String value) {
    codexCommand = value;
  }

  /** Retorna o modelo canônico. */
  public String getModel() {
    return model;
  }

  /** Define o modelo canônico. */
  public void setModel(String value) {
    model = value;
  }

  /** Retorna o modelo estratégico explícito, sem fallback silencioso ao modelo operacional. */
  public String getStrategicModel() {
    if (strategicModel == null || strategicModel.isBlank()) {
      throw new IllegalStateException(
          "Modelo estratégico obrigatório; configure CODEX_STRATEGIC_MODEL.");
    }
    return strategicModel.trim();
  }

  /** Configura o modelo das decisões estratégicas sem alterar o padrão operacional. */
  public void setStrategicModel(String strategicModel) {
    this.strategicModel = strategicModel;
  }

  /** Seleciona pelo contrato da atividade, sem inferir importância pelo texto ou pelo produto. */
  public String modelForActivity(String processCode, String activityId) {
    boolean strategic =
        "pde-commercial-plan-offer".equals(processCode) && "productArchitecture".equals(activityId);
    return strategic ? getStrategicModel() : getModel();
  }

  /** Retorna o esforço de raciocínio. */
  public String getReasoningEffort() {
    return reasoningEffort;
  }

  /** Define o esforço de raciocínio. */
  public void setReasoningEffort(String value) {
    reasoningEffort = value;
  }

  /** Exige raciocínio máximo antes de executar ou auditar qualquer chamada de Dédalo. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || !"max".equals(reasoningEffort.trim())) {
      throw new IllegalStateException(
          "CODEX_REASONING_EFFORT deve ser max em toda execução do Dédalo.");
    }
    return "max";
  }

  /** Retorna a identidade imutável do deploy atual. */
  public String getBuildReference() {
    return buildReference;
  }

  /** Define a identidade imutável do deploy atual. */
  public void setBuildReference(String value) {
    buildReference = value;
  }

  /** Retorna o diretório persistente dos resultados BPM ainda não confirmados. */
  public String getBpmStateDirectory() {
    return bpmStateDirectory;
  }

  /** Define armazenamento exclusivo do executor para recuperar callbacks após reinícios. */
  public void setBpmStateDirectory(String value) {
    bpmStateDirectory = value;
  }

  /** Retorna o timeout. */
  public Duration getCodexTimeout() {
    return codexTimeout;
  }

  /** Define o timeout. */
  public void setCodexTimeout(Duration value) {
    codexTimeout = value;
  }
}

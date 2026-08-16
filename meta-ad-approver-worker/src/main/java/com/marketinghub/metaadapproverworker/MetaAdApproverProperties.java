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
  private Duration backendConnectTimeout = Duration.ofSeconds(10);
  private Duration backendReadTimeout = Duration.ofSeconds(30);
  private int pendingLimit = 3;
  private String openAiBaseUrl = "https://api.openai.com/v1";
  private String openAiApiKey;
  private String openAiApiKeyFile = "/run/secrets/openai_api_key";
  private String imageModel = "gpt-image-2";
  private Duration imageTimeout = Duration.ofMinutes(3);
  private int imageStudioPendingLimit = 2;

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

  /** Retorna o limite para abrir conexão com o backend. */
  public Duration getBackendConnectTimeout() {
    return backendConnectTimeout;
  }

  /** Define o limite para abrir conexão com o backend. */
  public void setBackendConnectTimeout(Duration value) {
    backendConnectTimeout = value;
  }

  /** Retorna o limite de espera por resposta do backend. */
  public Duration getBackendReadTimeout() {
    return backendReadTimeout;
  }

  /** Define o limite de espera por resposta do backend. */
  public void setBackendReadTimeout(Duration value) {
    backendReadTimeout = value;
  }

  /** Retorna o limite do lote. */
  public int getPendingLimit() {
    return pendingLimit;
  }

  /** Define o limite do lote. */
  public void setPendingLimit(int value) {
    pendingLimit = Math.max(1, value);
  }

  /** Retorna a URL base oficial ou compatível da API OpenAI. */
  public String getOpenAiBaseUrl() {
    return openAiBaseUrl;
  }

  /** Define a URL base da API OpenAI. */
  public void setOpenAiBaseUrl(String value) {
    openAiBaseUrl = value;
  }

  /** Retorna a chave direta quando explicitamente configurada. */
  public String getOpenAiApiKey() {
    return openAiApiKey;
  }

  /** Define a chave direta da API OpenAI. */
  public void setOpenAiApiKey(String value) {
    openAiApiKey = value;
  }

  /** Retorna o arquivo seguro que contém a chave da OpenAI. */
  public String getOpenAiApiKeyFile() {
    return openAiApiKeyFile;
  }

  /** Define o arquivo seguro da chave da OpenAI. */
  public void setOpenAiApiKeyFile(String value) {
    openAiApiKeyFile = value;
  }

  /** Retorna o modelo visual obrigatório do estúdio. */
  public String getImageModel() {
    return imageModel;
  }

  /** Define o modelo visual do estúdio. */
  public void setImageModel(String value) {
    imageModel = value;
  }

  /** Retorna o limite de espera de uma criação ou edição. */
  public Duration getImageTimeout() {
    return imageTimeout;
  }

  /** Define o limite de espera de uma criação ou edição. */
  public void setImageTimeout(Duration value) {
    imageTimeout = value;
  }

  /** Retorna o lote máximo de produções e revisões visuais. */
  public int getImageStudioPendingLimit() {
    return imageStudioPendingLimit;
  }

  /** Define o lote máximo de produções e revisões visuais. */
  public void setImageStudioPendingLimit(int value) {
    imageStudioPendingLimit = Math.max(1, value);
  }
}

package com.marketinghub.financialagentworker;

import java.time.Duration;
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
  private String strategicModel = "gpt-6-astra";
  private String reasoningEffort = "max";
  private String serviceTier = "default";
  private String serviceTierExceptionReason =
      "O catálogo do Codex OAuth não anuncia Flex para os modelos disponíveis ao harness.";
  private Long commercialPlanId;
  private Duration codexTimeout = Duration.ofMinutes(40);

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

  /** Seleciona Astra nas projeções e hipóteses; preserva a conciliação de rotina. */
  public String modelForAuthority(String authorityMode) {
    return "READ_ONLY_REVENUE_PROJECTION".equals(authorityMode)
            || "COMMERCIAL_ASSUMPTIONS_VALIDATION".equals(authorityMode)
        ? getStrategicModel()
        : getModel();
  }

  /** Retorna max validado para chamadas e auditorias, recusando configuração inferior. */
  public String getReasoningEffort() {
    return requiredReasoningEffort();
  }

  /** Configura o esforço de raciocínio auditado. */
  public void setReasoningEffort(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }

  /** Exige max antes da chamada e mantém comando e auditoria com a mesma configuração. */
  public String requiredReasoningEffort() {
    if (reasoningEffort == null || !"max".equals(reasoningEffort.trim())) {
      throw new IllegalStateException(
          "CODEX_REASONING_EFFORT deve ser max, obrigatório para auditar Plutus.");
    }
    return "max";
  }

  /** Retorna o tier solicitado ao runtime Codex. */
  public String getServiceTier() {
    return serviceTier;
  }

  /** Configura o tier somente quando suportado pelo catálogo do Codex OAuth. */
  public void setServiceTier(String serviceTier) {
    this.serviceTier = serviceTier;
  }

  /** Retorna a justificativa auditável para a exceção ao modo Flex. */
  public String getServiceTierExceptionReason() {
    return serviceTierExceptionReason;
  }

  /** Configura a justificativa auditável para a exceção ao modo Flex. */
  public void setServiceTierExceptionReason(String serviceTierExceptionReason) {
    this.serviceTierExceptionReason = serviceTierExceptionReason;
  }

  /** Retorna o planejamento acompanhado diariamente. */
  public Long getCommercialPlanId() {
    return commercialPlanId;
  }

  /** Configura o planejamento acompanhado diariamente. */
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

package com.marketinghub.productdiscovery.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Representa um ciclo auditável de pesquisa de dores e oportunidades para produtos PDE. */
@Entity
@Table(name = "product_discovery_cycle")
public class ProductDiscoveryCycle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "theme", nullable = false, length = 191)
  private String theme;

  @Column(name = "target_audience", length = 191)
  private String targetAudience;

  @Column(name = "country", nullable = false, length = 16)
  private String country;

  @Column(name = "language", nullable = false, length = 16)
  private String language;

  @Column(name = "acquisition_channel", length = 120)
  private String acquisitionChannel;

  @Column(name = "commercial_constraints", columnDefinition = "LONGTEXT")
  private String commercialConstraints;

  @Column(name = "forbidden_categories", columnDefinition = "LONGTEXT")
  private String forbiddenCategories;

  @Column(name = "objective", columnDefinition = "LONGTEXT")
  private String objective;

  @Enumerated(EnumType.STRING)
  @Column(name = "research_mode", nullable = false, length = 32)
  private ProductDiscoveryResearchMode researchMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "market_type", nullable = false, length = 24)
  private ProductDiscoveryMarketType marketType;

  @Column(name = "reference_sources", columnDefinition = "LONGTEXT")
  private String referenceSources;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ProductDiscoveryCycleStatus status;

  @Column(name = "stage_code", nullable = false, length = 80)
  private String stageCode;

  @Column(name = "decision_summary", columnDefinition = "LONGTEXT")
  private String decisionSummary;

  @Column(name = "error_message", columnDefinition = "LONGTEXT")
  private String errorMessage;

  @Column(name = "execution_lease_id", length = 36)
  private String executionLeaseId;

  @Column(name = "lease_expires_at")
  private Instant leaseExpiresAt;

  @Column(name = "execution_attempt", nullable = false)
  private int executionAttempt;

  @Column(name = "research_plan_json", columnDefinition = "LONGTEXT")
  private String researchPlanJson;

  @Column(name = "research_plan_raw_response", columnDefinition = "LONGTEXT")
  private String researchPlanRawResponse;

  @Column(name = "research_plan_model", length = 120)
  private String researchPlanModel;

  @Column(name = "research_analysis_raw_response", columnDefinition = "LONGTEXT")
  private String researchAnalysisRawResponse;

  @Column(name = "research_analysis_model", length = 120)
  private String researchAnalysisModel;

  @Column(name = "research_evidence_report_json", columnDefinition = "LONGTEXT")
  private String researchEvidenceReportJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Preenche timestamps e valores padrão antes da criação. */
  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (status == null) {
      status = ProductDiscoveryCycleStatus.DRAFT;
    }
    if (stageCode == null) {
      stageCode = "research";
    }
    if (researchMode == null) {
      researchMode = ProductDiscoveryResearchMode.VALIDATE_MARKET;
    }
    if (marketType == null) {
      marketType = ProductDiscoveryMarketType.UNSPECIFIED;
    }
  }

  /** Atualiza o timestamp de alteração antes de salvar mudanças. */
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  /** Retorna o identificador do ciclo. */
  public Long getId() {
    return id;
  }

  /** Define o identificador do ciclo. */
  public void setId(Long id) {
    this.id = id;
  }

  /** Retorna o tema amplo da pesquisa. */
  public String getTheme() {
    return theme;
  }

  /** Define o tema amplo da pesquisa. */
  public void setTheme(String theme) {
    this.theme = theme;
  }

  /** Retorna o público-alvo desejado. */
  public String getTargetAudience() {
    return targetAudience;
  }

  /** Define o público-alvo desejado. */
  public void setTargetAudience(String targetAudience) {
    this.targetAudience = targetAudience;
  }

  /** Retorna o país da pesquisa. */
  public String getCountry() {
    return country;
  }

  /** Define o país da pesquisa. */
  public void setCountry(String country) {
    this.country = country;
  }

  /** Retorna o idioma da pesquisa. */
  public String getLanguage() {
    return language;
  }

  /** Define o idioma da pesquisa. */
  public void setLanguage(String language) {
    this.language = language;
  }

  /** Retorna o canal provável de aquisição. */
  public String getAcquisitionChannel() {
    return acquisitionChannel;
  }

  /** Define o canal provável de aquisição. */
  public void setAcquisitionChannel(String acquisitionChannel) {
    this.acquisitionChannel = acquisitionChannel;
  }

  /** Retorna as restrições comerciais do ciclo. */
  public String getCommercialConstraints() {
    return commercialConstraints;
  }

  /** Define as restrições comerciais do ciclo. */
  public void setCommercialConstraints(String commercialConstraints) {
    this.commercialConstraints = commercialConstraints;
  }

  /** Retorna categorias proibidas para a pesquisa. */
  public String getForbiddenCategories() {
    return forbiddenCategories;
  }

  /** Define categorias proibidas para a pesquisa. */
  public void setForbiddenCategories(String forbiddenCategories) {
    this.forbiddenCategories = forbiddenCategories;
  }

  /** Retorna o objetivo do ciclo. */
  public String getObjective() {
    return objective;
  }

  /** Define o objetivo do ciclo. */
  public void setObjective(String objective) {
    this.objective = objective;
  }

  /** Retorna se o ciclo descobre candidatas ou valida um mercado informado. */
  public ProductDiscoveryResearchMode getResearchMode() {
    return researchMode;
  }

  /** Define o modo explícito usado por Argos na pesquisa. */
  public void setResearchMode(ProductDiscoveryResearchMode researchMode) {
    this.researchMode = researchMode;
  }

  /** Retorna o tipo de comprador declarado no briefing. */
  public ProductDiscoveryMarketType getMarketType() {
    return marketType;
  }

  /** Define o tipo de comprador sem inferência pelo texto do tema. */
  public void setMarketType(ProductDiscoveryMarketType marketType) {
    this.marketType = marketType;
  }

  /** Retorna as fontes editoriais públicas usadas como contexto de pesquisa. */
  public String getReferenceSources() {
    return referenceSources;
  }

  /** Preserva as fontes editoriais declaradas pelo operador. */
  public void setReferenceSources(String referenceSources) {
    this.referenceSources = referenceSources;
  }

  /** Retorna o status operacional. */
  public ProductDiscoveryCycleStatus getStatus() {
    return status;
  }

  /** Define o status operacional. */
  public void setStatus(ProductDiscoveryCycleStatus status) {
    this.status = status;
  }

  /** Retorna a etapa atual. */
  public String getStageCode() {
    return stageCode;
  }

  /** Define a etapa atual. */
  public void setStageCode(String stageCode) {
    this.stageCode = stageCode;
  }

  /** Retorna o resumo da decisão do ciclo. */
  public String getDecisionSummary() {
    return decisionSummary;
  }

  /** Define o resumo da decisão do ciclo. */
  public void setDecisionSummary(String decisionSummary) {
    this.decisionSummary = decisionSummary;
  }

  /** Retorna a mensagem de erro do ciclo. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Define a mensagem de erro do ciclo. */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /** Retorna o identificador da reserva operacional vigente. */
  public String getExecutionLeaseId() {
    return executionLeaseId;
  }

  /** Define o identificador que protege os callbacks da execução vigente. */
  public void setExecutionLeaseId(String executionLeaseId) {
    this.executionLeaseId = executionLeaseId;
  }

  /** Retorna quando a reserva pode ser recuperada por outro worker. */
  public Instant getLeaseExpiresAt() {
    return leaseExpiresAt;
  }

  /** Define o limite da reserva operacional atual. */
  public void setLeaseExpiresAt(Instant leaseExpiresAt) {
    this.leaseExpiresAt = leaseExpiresAt;
  }

  /** Retorna quantas vezes o ciclo foi reservado para execução. */
  public int getExecutionAttempt() {
    return executionAttempt;
  }

  /** Atualiza o contador auditável de tentativas. */
  public void setExecutionAttempt(int executionAttempt) {
    this.executionAttempt = executionAttempt;
  }

  /** Retorna o plano dirigido e auditável definido por Argos. */
  public String getResearchPlanJson() {
    return researchPlanJson;
  }

  /** Persiste o plano dirigido definido antes da coleta. */
  public void setResearchPlanJson(String researchPlanJson) {
    this.researchPlanJson = researchPlanJson;
  }

  /** Retorna a resposta bruta que originou o plano. */
  public String getResearchPlanRawResponse() {
    return researchPlanRawResponse;
  }

  /** Preserva a resposta bruta para auditoria do agente. */
  public void setResearchPlanRawResponse(String researchPlanRawResponse) {
    this.researchPlanRawResponse = researchPlanRawResponse;
  }

  /** Retorna o modelo responsável pelo plano de investigação. */
  public String getResearchPlanModel() {
    return researchPlanModel;
  }

  /** Registra o modelo responsável pelo plano de investigação. */
  public void setResearchPlanModel(String researchPlanModel) {
    this.researchPlanModel = researchPlanModel;
  }

  /** Retorna a resposta bruta da síntese factual posterior à coleta. */
  public String getResearchAnalysisRawResponse() {
    return researchAnalysisRawResponse;
  }

  /** Preserva a resposta bruta da síntese factual para auditoria. */
  public void setResearchAnalysisRawResponse(String researchAnalysisRawResponse) {
    this.researchAnalysisRawResponse = researchAnalysisRawResponse;
  }

  /** Retorna o modelo responsável pela síntese factual. */
  public String getResearchAnalysisModel() {
    return researchAnalysisModel;
  }

  /** Registra o modelo responsável pela síntese factual. */
  public void setResearchAnalysisModel(String researchAnalysisModel) {
    this.researchAnalysisModel = researchAnalysisModel;
  }

  /** Retorna o relatório consolidado de fontes e gates da coleta. */
  public String getResearchEvidenceReportJson() {
    return researchEvidenceReportJson;
  }

  /** Persiste o relatório estruturado sem serializá-lo dentro do callback JSON. */
  public void setResearchEvidenceReportJson(String researchEvidenceReportJson) {
    this.researchEvidenceReportJson = researchEvidenceReportJson;
  }

  /** Retorna a data de criação. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Retorna a data de atualização. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

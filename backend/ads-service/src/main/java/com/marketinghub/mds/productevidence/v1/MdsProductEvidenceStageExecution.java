package com.marketinghub.mds.productevidence.v1;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Responsabilidade: representar uma execução auditável do pipeline MDS de evidência científica de
 * produto.
 */
@Entity
@Table(name = "mds_product_evidence_stage_execution")
public class MdsProductEvidenceStageExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "job_id", nullable = false, length = 64)
  private String jobId;

  @Column(name = "market_niche_id", nullable = false)
  private Long marketNicheId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "market_niche_id", nullable = false, insertable = false, updatable = false)
  private MarketNiche marketNiche;

  @Column(name = "stage_code", nullable = false, length = 100)
  private String stageCode;

  @Column(name = "status", nullable = false, length = 50)
  private String status;

  @Column(name = "product_idea", nullable = false, columnDefinition = "LONGTEXT")
  private String productIdea;

  @Column(name = "scientific_question", nullable = false, columnDefinition = "LONGTEXT")
  private String scientificQuestion;

  @Column(name = "input_payload", nullable = false, columnDefinition = "LONGTEXT")
  private String inputPayload;

  @Column(name = "output_payload", columnDefinition = "LONGTEXT")
  private String outputPayload;

  @Column(name = "artifacts_payload", columnDefinition = "LONGTEXT")
  private String artifactsPayload;

  @Column(name = "root_cause", columnDefinition = "LONGTEXT")
  private String rootCause;

  @Column(name = "commercial_impact", columnDefinition = "LONGTEXT")
  private String commercialImpact;

  @Column(name = "recommended_action", columnDefinition = "LONGTEXT")
  private String recommendedAction;

  @Column(name = "error_type", length = 191)
  private String errorType;

  @Column(name = "error_message", columnDefinition = "LONGTEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "processing_started_at")
  private Instant processingStartedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  /** Retorna o identificador técnico da execução científica. */
  public Long getId() {
    return id;
  }

  /** Define o identificador técnico da execução científica. */
  public void setId(Long id) {
    this.id = id;
  }

  /** Retorna o job de correlação do pacote científico. */
  public String getJobId() {
    return jobId;
  }

  /** Define o job de correlação do pacote científico. */
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  /** Retorna o nicho de mercado associado. */
  public Long getMarketNicheId() {
    return marketNicheId;
  }

  /** Define o nicho de mercado associado. */
  public void setMarketNicheId(Long marketNicheId) {
    this.marketNicheId = marketNicheId;
  }

  /** Retorna a entidade de nicho carregada sob demanda. */
  public MarketNiche getMarketNiche() {
    return marketNiche;
  }

  /** Define a entidade de nicho carregada sob demanda. */
  public void setMarketNiche(MarketNiche marketNiche) {
    this.marketNiche = marketNiche;
  }

  /** Retorna o código canônico da etapa científica. */
  public String getStageCode() {
    return stageCode;
  }

  /** Define o código canônico da etapa científica. */
  public void setStageCode(String stageCode) {
    this.stageCode = stageCode;
  }

  /** Retorna o status operacional persistido. */
  public String getStatus() {
    return status;
  }

  /** Define o status operacional persistido. */
  public void setStatus(String status) {
    this.status = status;
  }

  /** Retorna a ideia de produto usada na busca científica. */
  public String getProductIdea() {
    return productIdea;
  }

  /** Define a ideia de produto usada na busca científica. */
  public void setProductIdea(String productIdea) {
    this.productIdea = productIdea;
  }

  /** Retorna a pergunta científica usada na busca. */
  public String getScientificQuestion() {
    return scientificQuestion;
  }

  /** Define a pergunta científica usada na busca. */
  public void setScientificQuestion(String scientificQuestion) {
    this.scientificQuestion = scientificQuestion;
  }

  /** Retorna o payload funcional de entrada da etapa. */
  public String getInputPayload() {
    return inputPayload;
  }

  /** Define o payload funcional de entrada da etapa. */
  public void setInputPayload(String inputPayload) {
    this.inputPayload = inputPayload;
  }

  /** Retorna o payload funcional produzido pela etapa. */
  public String getOutputPayload() {
    return outputPayload;
  }

  /** Define o payload funcional produzido pela etapa. */
  public void setOutputPayload(String outputPayload) {
    this.outputPayload = outputPayload;
  }

  /** Retorna os artefatos auditáveis produzidos pela etapa. */
  public String getArtifactsPayload() {
    return artifactsPayload;
  }

  /** Define os artefatos auditáveis produzidos pela etapa. */
  public void setArtifactsPayload(String artifactsPayload) {
    this.artifactsPayload = artifactsPayload;
  }

  /** Retorna a causa-raiz funcional quando a etapa bloqueia. */
  public String getRootCause() {
    return rootCause;
  }

  /** Define a causa-raiz funcional quando a etapa bloqueia. */
  public void setRootCause(String rootCause) {
    this.rootCause = rootCause;
  }

  /** Retorna o impacto comercial do bloqueio científico. */
  public String getCommercialImpact() {
    return commercialImpact;
  }

  /** Define o impacto comercial do bloqueio científico. */
  public void setCommercialImpact(String commercialImpact) {
    this.commercialImpact = commercialImpact;
  }

  /** Retorna a ação recomendada para destravar o pacote científico. */
  public String getRecommendedAction() {
    return recommendedAction;
  }

  /** Define a ação recomendada para destravar o pacote científico. */
  public void setRecommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
  }

  /** Retorna o tipo técnico de erro informado pelo worker. */
  public String getErrorType() {
    return errorType;
  }

  /** Define o tipo técnico de erro informado pelo worker. */
  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  /** Retorna a mensagem técnica de erro informada pelo worker. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Define a mensagem técnica de erro informada pelo worker. */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /** Retorna a data de criação da execução. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Define a data de criação da execução. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Retorna quando a execução foi entregue ao worker. */
  public Instant getProcessingStartedAt() {
    return processingStartedAt;
  }

  /** Define quando a execução foi entregue ao worker. */
  public void setProcessingStartedAt(Instant processingStartedAt) {
    this.processingStartedAt = processingStartedAt;
  }

  /** Retorna quando a execução foi finalizada. */
  public Instant getCompletedAt() {
    return completedAt;
  }

  /** Define quando a execução foi finalizada. */
  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }
}

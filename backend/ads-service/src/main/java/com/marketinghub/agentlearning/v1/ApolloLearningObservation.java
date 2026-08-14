package com.marketinghub.agentlearning.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: persistir uma observação sombra de Apolo sem efeito financeiro ou editorial.
 */
@Entity
@Table(name = "apollo_learning_observation")
public class ApolloLearningObservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", nullable = false, unique = true)
  private Long jobId;

  @Column(name = "scope_id", nullable = false, length = 120)
  private String scopeId;

  @Column(name = "baseline_version", nullable = false, length = 80)
  private String baselineVersion;

  @Column(name = "candidate_version", nullable = false, length = 80)
  private String candidateVersion;

  @Column(name = "baseline_score", nullable = false, precision = 8, scale = 4)
  private BigDecimal baselineScore;

  @Column(name = "candidate_score", nullable = false, precision = 8, scale = 4)
  private BigDecimal candidateScore;

  @Column(name = "baseline_cost", nullable = false, precision = 12, scale = 4)
  private BigDecimal baselineCost;

  @Column(name = "candidate_cost", nullable = false, precision = 12, scale = 4)
  private BigDecimal candidateCost;

  @Lob
  @Column(name = "comparison_json", nullable = false, columnDefinition = "LONGTEXT")
  private String comparisonJson;

  @Column(name = "qa_reviewer", nullable = false, length = 80)
  private String qaReviewer;

  @Column(name = "qa_passed", nullable = false)
  private boolean qaPassed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Retorna o identificador. */
  public Long getId() {
    return id;
  }

  /** Retorna o job. */
  public Long getJobId() {
    return jobId;
  }

  /** Define o job. */
  public void setJobId(Long value) {
    jobId = value;
  }

  /** Retorna o escopo. */
  public String getScopeId() {
    return scopeId;
  }

  /** Define o escopo. */
  public void setScopeId(String value) {
    scopeId = value;
  }

  /** Retorna a baseline. */
  public String getBaselineVersion() {
    return baselineVersion;
  }

  /** Define a baseline. */
  public void setBaselineVersion(String value) {
    baselineVersion = value;
  }

  /** Retorna a candidata. */
  public String getCandidateVersion() {
    return candidateVersion;
  }

  /** Define a candidata. */
  public void setCandidateVersion(String value) {
    candidateVersion = value;
  }

  /** Retorna a nota baseline. */
  public BigDecimal getBaselineScore() {
    return baselineScore;
  }

  /** Define a nota baseline. */
  public void setBaselineScore(BigDecimal value) {
    baselineScore = value;
  }

  /** Retorna a nota candidata. */
  public BigDecimal getCandidateScore() {
    return candidateScore;
  }

  /** Define a nota candidata. */
  public void setCandidateScore(BigDecimal value) {
    candidateScore = value;
  }

  /** Retorna o custo baseline. */
  public BigDecimal getBaselineCost() {
    return baselineCost;
  }

  /** Define o custo baseline. */
  public void setBaselineCost(BigDecimal value) {
    baselineCost = value;
  }

  /** Retorna o custo candidato. */
  public BigDecimal getCandidateCost() {
    return candidateCost;
  }

  /** Define o custo candidato. */
  public void setCandidateCost(BigDecimal value) {
    candidateCost = value;
  }

  /** Retorna a comparação. */
  public String getComparisonJson() {
    return comparisonJson;
  }

  /** Define a comparação. */
  public void setComparisonJson(String value) {
    comparisonJson = value;
  }

  /** Retorna o revisor independente. */
  public String getQaReviewer() {
    return qaReviewer;
  }

  /** Define o revisor independente. */
  public void setQaReviewer(String value) {
    qaReviewer = value;
  }

  /** Informa se o QA passou. */
  public boolean isQaPassed() {
    return qaPassed;
  }

  /** Define o resultado de QA. */
  public void setQaPassed(boolean value) {
    qaPassed = value;
  }

  /** Retorna a criação. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Define a criação. */
  public void setCreatedAt(Instant value) {
    createdAt = value;
  }
}

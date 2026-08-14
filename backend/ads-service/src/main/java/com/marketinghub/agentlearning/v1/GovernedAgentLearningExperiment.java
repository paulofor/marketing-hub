package com.marketinghub.agentlearning.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: persistir uma comparação governada entre uma estratégia ativa e uma candidata.
 */
@Entity
@Table(name = "governed_agent_learning_experiment")
public class GovernedAgentLearningExperiment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "agent_key", nullable = false, length = 80)
  private String agentKey;

  @Column(name = "scope_type", nullable = false, length = 60)
  private String scopeType;

  @Column(name = "scope_id", nullable = false, length = 120)
  private String scopeId;

  @Column(name = "memory_id", nullable = false)
  private Long memoryId;

  @Column(name = "candidate_version", nullable = false, length = 80)
  private String candidateVersion;

  @Column(name = "baseline_version", nullable = false, length = 80)
  private String baselineVersion;

  @Lob
  @Column(name = "frozen_replay_set_json", nullable = false, columnDefinition = "LONGTEXT")
  private String frozenReplaySetJson;

  @Lob
  @Column(name = "holdout_replay_set_json", nullable = false, columnDefinition = "LONGTEXT")
  private String holdoutReplaySetJson;

  @Lob
  @Column(name = "baseline_result_json", columnDefinition = "LONGTEXT")
  private String baselineResultJson;

  @Lob
  @Column(name = "candidate_result_json", columnDefinition = "LONGTEXT")
  private String candidateResultJson;

  @Lob
  @Column(name = "decision_evidence", columnDefinition = "LONGTEXT")
  private String decisionEvidence;

  @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Column(name = "minimum_gain", nullable = false, precision = 8, scale = 4)
  private BigDecimal minimumGain;

  @Column(name = "maximum_cost_increase_ratio", nullable = false, precision = 8, scale = 4)
  private BigDecimal maximumCostIncreaseRatio;

  @Column(name = "regression_passed", nullable = false)
  private boolean regressionPassed;

  @Column(name = "local_validation_passed", nullable = false)
  private boolean localValidationPassed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "evaluated_at")
  private Instant evaluatedAt;

  @Column(name = "promoted_at")
  private Instant promotedAt;

  /** Retorna o identificador. */
  public Long getId() {
    return id;
  }

  /** Retorna o agente. */
  public String getAgentKey() {
    return agentKey;
  }

  /** Define o agente. */
  public void setAgentKey(String value) {
    agentKey = value;
  }

  /** Retorna o tipo de escopo. */
  public String getScopeType() {
    return scopeType;
  }

  /** Define o tipo de escopo. */
  public void setScopeType(String value) {
    scopeType = value;
  }

  /** Retorna o escopo. */
  public String getScopeId() {
    return scopeId;
  }

  /** Define o escopo. */
  public void setScopeId(String value) {
    scopeId = value;
  }

  /** Retorna a memória candidata. */
  public Long getMemoryId() {
    return memoryId;
  }

  /** Define a memória candidata. */
  public void setMemoryId(Long value) {
    memoryId = value;
  }

  /** Retorna a versão candidata. */
  public String getCandidateVersion() {
    return candidateVersion;
  }

  /** Define a versão candidata. */
  public void setCandidateVersion(String value) {
    candidateVersion = value;
  }

  /** Retorna a versão baseline. */
  public String getBaselineVersion() {
    return baselineVersion;
  }

  /** Define a versão baseline. */
  public void setBaselineVersion(String value) {
    baselineVersion = value;
  }

  /** Retorna o replay congelado. */
  public String getFrozenReplaySetJson() {
    return frozenReplaySetJson;
  }

  /** Define o replay congelado. */
  public void setFrozenReplaySetJson(String value) {
    frozenReplaySetJson = value;
  }

  /** Retorna o holdout congelado. */
  public String getHoldoutReplaySetJson() {
    return holdoutReplaySetJson;
  }

  /** Define o holdout congelado. */
  public void setHoldoutReplaySetJson(String value) {
    holdoutReplaySetJson = value;
  }

  /** Retorna o resultado baseline. */
  public String getBaselineResultJson() {
    return baselineResultJson;
  }

  /** Define o resultado baseline. */
  public void setBaselineResultJson(String value) {
    baselineResultJson = value;
  }

  /** Retorna o resultado candidato. */
  public String getCandidateResultJson() {
    return candidateResultJson;
  }

  /** Define o resultado candidato. */
  public void setCandidateResultJson(String value) {
    candidateResultJson = value;
  }

  /** Retorna a evidência da decisão. */
  public String getDecisionEvidence() {
    return decisionEvidence;
  }

  /** Define a evidência da decisão. */
  public void setDecisionEvidence(String value) {
    decisionEvidence = value;
  }

  /** Retorna o estado. */
  public String getStatus() {
    return status;
  }

  /** Define o estado. */
  public void setStatus(String value) {
    status = value;
  }

  /** Retorna o ganho mínimo. */
  public BigDecimal getMinimumGain() {
    return minimumGain;
  }

  /** Define o ganho mínimo. */
  public void setMinimumGain(BigDecimal value) {
    minimumGain = value;
  }

  /** Retorna o limite de custo. */
  public BigDecimal getMaximumCostIncreaseRatio() {
    return maximumCostIncreaseRatio;
  }

  /** Define o limite de custo. */
  public void setMaximumCostIncreaseRatio(BigDecimal value) {
    maximumCostIncreaseRatio = value;
  }

  /** Informa se a regressão passou. */
  public boolean isRegressionPassed() {
    return regressionPassed;
  }

  /** Define a regressão. */
  public void setRegressionPassed(boolean value) {
    regressionPassed = value;
  }

  /** Informa se a validação local passou. */
  public boolean isLocalValidationPassed() {
    return localValidationPassed;
  }

  /** Define a validação local. */
  public void setLocalValidationPassed(boolean value) {
    localValidationPassed = value;
  }

  /** Retorna a criação. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Define a criação. */
  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  /** Define a avaliação. */
  public void setEvaluatedAt(Instant value) {
    evaluatedAt = value;
  }

  /** Retorna a avaliação. */
  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }

  /** Define a promoção. */
  public void setPromotedAt(Instant value) {
    promotedAt = value;
  }

  /** Retorna a promoção. */
  public Instant getPromotedAt() {
    return promotedAt;
  }
}

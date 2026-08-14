package com.marketinghub.agentlearning.v1;

import jakarta.persistence.*;
import java.time.Instant;

/** Responsabilidade: persistir uma skill candidata versionada e seu ciclo seguro de vida. */
@Entity
@Table(name = "governed_agent_skill_candidate")
public class GovernedAgentSkillCandidate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "agent_key", nullable = false, length = 80)
  private String agentKey;

  @Column(name = "skill_key", nullable = false, length = 120)
  private String skillKey;

  @Column(name = "baseline_version", nullable = false, length = 80)
  private String baselineVersion;

  @Column(name = "candidate_version", nullable = false, length = 80)
  private String candidateVersion;

  @Lob
  @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
  private String content;

  @Lob
  @Column(name = "diff_summary", nullable = false, columnDefinition = "LONGTEXT")
  private String diffSummary;

  @Lob
  @Column(name = "provenance_json", nullable = false, columnDefinition = "LONGTEXT")
  private String provenanceJson;

  @Column(name = "safety_decision", nullable = false, length = 40)
  private String safetyDecision;

  @Lob
  @Column(name = "safety_evidence", nullable = false, columnDefinition = "LONGTEXT")
  private String safetyEvidence;

  @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Column(name = "promoted_at")
  private Instant promotedAt;

  @Column(name = "rolled_back_at")
  private Instant rolledBackAt;

  @Lob
  @Column(name = "rollback_reason", columnDefinition = "LONGTEXT")
  private String rollbackReason;

  @Column(name = "monitored_cases", nullable = false)
  private int monitoredCases;

  @Column(name = "approved_cases", nullable = false)
  private int approvedCases;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Retorna o identificador. */
  public Long getId() {
    return id;
  }

  /** Retorna o experimento. */
  public Long getExperimentId() {
    return experimentId;
  }

  /** Define o experimento. */
  public void setExperimentId(Long v) {
    experimentId = v;
  }

  /** Retorna o agente. */
  public String getAgentKey() {
    return agentKey;
  }

  /** Define o agente. */
  public void setAgentKey(String v) {
    agentKey = v;
  }

  /** Retorna a skill. */
  public String getSkillKey() {
    return skillKey;
  }

  /** Define a skill. */
  public void setSkillKey(String v) {
    skillKey = v;
  }

  /** Retorna a baseline. */
  public String getBaselineVersion() {
    return baselineVersion;
  }

  /** Define a baseline. */
  public void setBaselineVersion(String v) {
    baselineVersion = v;
  }

  /** Retorna a candidata. */
  public String getCandidateVersion() {
    return candidateVersion;
  }

  /** Define a candidata. */
  public void setCandidateVersion(String v) {
    candidateVersion = v;
  }

  /** Retorna o conteúdo. */
  public String getContent() {
    return content;
  }

  /** Define o conteúdo. */
  public void setContent(String v) {
    content = v;
  }

  /** Retorna o resumo do diff. */
  public String getDiffSummary() {
    return diffSummary;
  }

  /** Define o resumo do diff. */
  public void setDiffSummary(String v) {
    diffSummary = v;
  }

  /** Retorna a procedência. */
  public String getProvenanceJson() {
    return provenanceJson;
  }

  /** Define a procedência. */
  public void setProvenanceJson(String v) {
    provenanceJson = v;
  }

  /** Retorna a decisão de segurança. */
  public String getSafetyDecision() {
    return safetyDecision;
  }

  /** Define a decisão de segurança. */
  public void setSafetyDecision(String v) {
    safetyDecision = v;
  }

  /** Retorna a evidência de segurança. */
  public String getSafetyEvidence() {
    return safetyEvidence;
  }

  /** Define a evidência de segurança. */
  public void setSafetyEvidence(String v) {
    safetyEvidence = v;
  }

  /** Retorna o estado. */
  public String getStatus() {
    return status;
  }

  /** Define o estado. */
  public void setStatus(String v) {
    status = v;
  }

  /** Retorna a promoção. */
  public Instant getPromotedAt() {
    return promotedAt;
  }

  /** Define a promoção. */
  public void setPromotedAt(Instant v) {
    promotedAt = v;
  }

  /** Retorna o rollback. */
  public Instant getRolledBackAt() {
    return rolledBackAt;
  }

  /** Define o rollback. */
  public void setRolledBackAt(Instant v) {
    rolledBackAt = v;
  }

  /** Retorna a causa do rollback. */
  public String getRollbackReason() {
    return rollbackReason;
  }

  /** Define a causa do rollback. */
  public void setRollbackReason(String v) {
    rollbackReason = v;
  }

  /** Retorna os casos monitorados. */
  public int getMonitoredCases() {
    return monitoredCases;
  }

  /** Define os casos monitorados. */
  public void setMonitoredCases(int v) {
    monitoredCases = v;
  }

  /** Retorna os casos aprovados. */
  public int getApprovedCases() {
    return approvedCases;
  }

  /** Define os casos aprovados. */
  public void setApprovedCases(int v) {
    approvedCases = v;
  }

  /** Retorna a criação. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Define a criação. */
  public void setCreatedAt(Instant v) {
    createdAt = v;
  }

  /** Retorna a atualização. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Define a atualização. */
  public void setUpdatedAt(Instant v) {
    updatedAt = v;
  }
}

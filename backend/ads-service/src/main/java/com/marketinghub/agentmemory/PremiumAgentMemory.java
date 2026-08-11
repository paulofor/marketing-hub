package com.marketinghub.agentmemory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: representar uma memória textual auditável e segregada de um agente premium. */
@Entity
@Table(name = "premium_agent_memory")
public class PremiumAgentMemory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "agent_key", nullable = false, length = 80)
  private String agentKey;

  @Column(name = "tenant_key", nullable = false, length = 120)
  private String tenantKey;

  @Column(name = "scope_type", nullable = false, length = 60)
  private String scopeType;

  @Column(name = "scope_id", nullable = false, length = 120)
  private String scopeId;

  @Column(name = "specialty", nullable = false, length = 120)
  private String specialty;

  @Lob
  @Column(name = "content_text", nullable = false, columnDefinition = "LONGTEXT")
  private String content;

  @Lob
  @Column(name = "evidence_text", nullable = false, columnDefinition = "LONGTEXT")
  private String evidence;

  @Column(name = "source_reference", length = 700)
  private String sourceReference;

  @Column(name = "source_execution_id", nullable = false, length = 120)
  private String sourceExecutionId;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "content_sha256", nullable = false, length = 64)
  private String contentSha256;

  @Column(name = "contract_version", nullable = false, length = 30)
  private String contractVersion;

  @Column(name = "valid_until")
  private Instant validUntil;

  @Column(name = "retrieval_count", nullable = false)
  private long retrievalCount;

  @Column(name = "last_retrieved_at")
  private Instant lastRetrievedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Incrementa o uso auditável da memória. */
  public void markRetrieved(Instant now) {
    retrievalCount++;
    lastRetrievedAt = now;
    updatedAt = now;
  }

  /** Retorna o identificador. */
  public Long getId() {
    return id;
  }

  /** Retorna o agente proprietário. */
  public String getAgentKey() {
    return agentKey;
  }

  /** Define o agente proprietário. */
  public void setAgentKey(String value) {
    agentKey = value;
  }

  /** Retorna o tenant. */
  public String getTenantKey() {
    return tenantKey;
  }

  /** Define o tenant. */
  public void setTenantKey(String value) {
    tenantKey = value;
  }

  /** Retorna o tipo de escopo. */
  public String getScopeType() {
    return scopeType;
  }

  /** Define o tipo de escopo. */
  public void setScopeType(String value) {
    scopeType = value;
  }

  /** Retorna o identificador de escopo. */
  public String getScopeId() {
    return scopeId;
  }

  /** Define o identificador de escopo. */
  public void setScopeId(String value) {
    scopeId = value;
  }

  /** Retorna a especialidade. */
  public String getSpecialty() {
    return specialty;
  }

  /** Define a especialidade. */
  public void setSpecialty(String value) {
    specialty = value;
  }

  /** Retorna o conhecimento conciso. */
  public String getContent() {
    return content;
  }

  /** Define o conhecimento conciso. */
  public void setContent(String value) {
    content = value;
  }

  /** Retorna a evidência. */
  public String getEvidence() {
    return evidence;
  }

  /** Define a evidência. */
  public void setEvidence(String value) {
    evidence = value;
  }

  /** Retorna a fonte. */
  public String getSourceReference() {
    return sourceReference;
  }

  /** Define a fonte. */
  public void setSourceReference(String value) {
    sourceReference = value;
  }

  /** Retorna a execução de origem. */
  public String getSourceExecutionId() {
    return sourceExecutionId;
  }

  /** Define a execução de origem. */
  public void setSourceExecutionId(String value) {
    sourceExecutionId = value;
  }

  /** Retorna o estado. */
  public String getStatus() {
    return status;
  }

  /** Define o estado. */
  public void setStatus(String value) {
    status = value;
  }

  /** Retorna a confiança. */
  public BigDecimal getConfidence() {
    return confidence;
  }

  /** Define a confiança. */
  public void setConfidence(BigDecimal value) {
    confidence = value;
  }

  /** Retorna o checksum. */
  public String getContentSha256() {
    return contentSha256;
  }

  /** Define o checksum. */
  public void setContentSha256(String value) {
    contentSha256 = value;
  }

  /** Retorna a versão do contrato. */
  public String getContractVersion() {
    return contractVersion;
  }

  /** Define a versão do contrato. */
  public void setContractVersion(String value) {
    contractVersion = value;
  }

  /** Retorna a validade. */
  public Instant getValidUntil() {
    return validUntil;
  }

  /** Define a validade. */
  public void setValidUntil(Instant value) {
    validUntil = value;
  }

  /** Retorna a quantidade de recuperações. */
  public long getRetrievalCount() {
    return retrievalCount;
  }

  /** Define a quantidade de recuperações. */
  public void setRetrievalCount(long value) {
    retrievalCount = value;
  }

  /** Retorna a última recuperação. */
  public Instant getLastRetrievedAt() {
    return lastRetrievedAt;
  }

  /** Retorna a criação. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Define a criação. */
  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  /** Retorna a atualização. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Define a atualização. */
  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }
}

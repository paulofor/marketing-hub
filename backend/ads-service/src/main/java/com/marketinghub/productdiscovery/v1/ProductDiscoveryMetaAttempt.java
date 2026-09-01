package com.marketinghub.productdiscovery.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Responsabilidade: preservar a investigação Meta imutável de cada tentativa do ciclo. */
@Entity
@Table(
    name = "product_discovery_meta_attempt",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_product_discovery_meta_attempt",
          columnNames = {"cycle_id", "attempt_number"}),
      @UniqueConstraint(
          name = "uk_product_discovery_meta_query_attempt",
          columnNames = {"cycle_id", "search_query"})
    })
public class ProductDiscoveryMetaAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cycle_id", nullable = false)
  private ProductDiscoveryCycle cycle;

  @Column(name = "attempt_number", nullable = false)
  private int attemptNumber;

  @Column(name = "investigation_id", nullable = false)
  private Long investigationId;

  @Column(name = "execution_lease_id", nullable = false, length = 36)
  private String executionLeaseId;

  @Column(name = "search_query", nullable = false, length = 60)
  private String searchQuery;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Construtor exigido pelo JPA. */
  protected ProductDiscoveryMetaAttempt() {}

  /** Cria o vínculo auditável de uma tentativa antes de executar o navegador público. */
  public ProductDiscoveryMetaAttempt(
      ProductDiscoveryCycle cycle,
      int attemptNumber,
      Long investigationId,
      String executionLeaseId,
      String searchQuery) {
    this.cycle = cycle;
    this.attemptNumber = attemptNumber;
    this.investigationId = investigationId;
    this.executionLeaseId = executionLeaseId;
    this.searchQuery = searchQuery;
  }

  /** Preenche o instante de criação quando o vínculo é persistido. */
  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
  }

  /** Retorna o identificador do vínculo. */
  public Long getId() {
    return id;
  }

  /** Retorna o ciclo que contém a tentativa. */
  public ProductDiscoveryCycle getCycle() {
    return cycle;
  }

  /** Retorna a posição da tentativa na ampliação controlada. */
  public int getAttemptNumber() {
    return attemptNumber;
  }

  /** Retorna a investigação Meta exclusiva da tentativa. */
  public Long getInvestigationId() {
    return investigationId;
  }

  /** Retorna o lease que criou originalmente o vínculo. */
  public String getExecutionLeaseId() {
    return executionLeaseId;
  }

  /** Retorna a consulta curta e imutável da tentativa. */
  public String getSearchQuery() {
    return searchQuery;
  }

  /** Retorna o instante em que o vínculo foi criado. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}

package com.marketinghub.product;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Responsabilidade: preservar cada mudança auditável do controle PLAY/STOP de um produto. */
@Entity
@Table(name = "product_automatic_execution_control_event")
@Getter
@NoArgsConstructor
public class ProductAutomaticExecutionControlEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "automatic_execution_enabled", nullable = false)
  private boolean automaticExecutionEnabled;

  @Column(name = "changed_by", nullable = false, length = 100)
  private String changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Cria um evento imutável com a decisão operacional e sua autoria. */
  public ProductAutomaticExecutionControlEvent(
      Product product, boolean automaticExecutionEnabled, String changedBy, Instant changedAt) {
    this.product = product;
    this.automaticExecutionEnabled = automaticExecutionEnabled;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }
}

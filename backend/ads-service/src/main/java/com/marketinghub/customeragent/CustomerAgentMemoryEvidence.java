package com.marketinghub.customeragent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: manter no MySQL a procedencia de uma evidencia pesada armazenada no S3. */
@Getter
@Setter
@Entity
@Table(
    name = "customer_agent_memory_evidence",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_customer_memory_evidence_dedup",
            columnNames = {"persona_id", "memory_layer", "sha256"}))
public class CustomerAgentMemoryEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id", nullable = false)
  private CustomerPersona persona;

  @ManyToOne
  @JoinColumn(name = "observation_id")
  private CustomerDigitalObservation observation;

  @Column(name = "memory_layer", nullable = false)
  private String memoryLayer;

  @Column(name = "source_url", columnDefinition = "TEXT")
  private String sourceUrl;

  @Column(name = "object_key", nullable = false, length = 700)
  private String objectKey;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "retention_until", nullable = false)
  private Instant retentionUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Registra o horario de criacao sem depender de valor enviado pelo cliente. */
  @PrePersist
  void initialize() {
    createdAt = Instant.now();
  }
}

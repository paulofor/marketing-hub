package com.marketinghub.experimentstrategist.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: manter a procedencia de um artefato anonimizado armazenado no S3. */
@Getter
@Setter
@Entity
@Table(name = "experiment_strategist_memory_artifact")
public class ExperimentStrategistMemoryArtifact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "memory_id", nullable = false)
  private ExperimentStrategistMemory memory;

  @Column(name = "artifact_type", nullable = false)
  private String artifactType;

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

  @Column(name = "anonymization_version", nullable = false)
  private String anonymizationVersion;

  @Column(name = "retention_until", nullable = false)
  private Instant retentionUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Registra o horario de criacao do artefato. */
  @PrePersist
  void initialize() {
    createdAt = Instant.now();
  }
}

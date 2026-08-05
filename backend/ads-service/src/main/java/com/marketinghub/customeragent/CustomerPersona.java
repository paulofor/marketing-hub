package com.marketinghub.customeragent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir uma versao auditavel de persona sustentada por evidencias. */
@Getter
@Setter
@Entity
@Table(name = "customer_persona")
public class CustomerPersona {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "persona_key", nullable = false)
  private String personaKey;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "confidence_level", nullable = false)
  private String confidenceLevel;

  @Column(name = "life_context", nullable = false, columnDefinition = "TEXT")
  private String lifeContext;

  @Column(name = "pain", nullable = false, columnDefinition = "TEXT")
  private String pain;

  @Column(name = "desired_progress", nullable = false, columnDefinition = "TEXT")
  private String desiredProgress;

  @Column(name = "awareness_level")
  private String awarenessLevel;

  @Column(name = "objections", columnDefinition = "LONGTEXT")
  private String objections;

  @Column(name = "trust_criteria", columnDefinition = "LONGTEXT")
  private String trustCriteria;

  @Column(name = "language_samples", columnDefinition = "LONGTEXT")
  private String languageSamples;

  @Column(name = "evidence_json", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa versao, estado e horarios da persona. */
  @PrePersist
  void initialize() {
    Instant now = Instant.now();
    if (versionNumber == null) versionNumber = 1;
    active = true;
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horario de auditoria. */
  @PreUpdate
  void updateTimestamp() {
    updatedAt = Instant.now();
  }
}

package com.marketinghub.experiment.history;

import com.marketinghub.experiment.Experiment;
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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: preservar um fato auditável ocorrido durante um experimento comercial. */
@Entity
@Table(name = "experiment_history_event")
@Getter
@Setter
@NoArgsConstructor
public class ExperimentHistoryEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  @Column(name = "category", nullable = false, length = 32)
  private String category;

  @Column(name = "title", nullable = false, length = 191)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "LONGTEXT")
  private String description;

  @Column(name = "evidence_json", columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "source", nullable = false, length = 191)
  private String source;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Aplica datas e origem padrão antes da primeira gravação. */
  @PrePersist
  void applyDefaults() {
    if (occurredAt == null) occurredAt = Instant.now();
    if (createdAt == null) createdAt = Instant.now();
    if (source == null || source.isBlank()) source = "USUARIO";
  }
}

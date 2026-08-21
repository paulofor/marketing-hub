package com.marketinghub.experimentstrategist;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: persistir a requisição e a resposta agregada de comportamento do Clarity. */
@Getter
@Setter
@Entity
@Table(name = "experiment_strategist_behavioral_snapshot")
public class ExperimentStrategistBehavioralSnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "execution_id", nullable = false)
  private ExperimentStrategistExecution execution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  @Column(name = "provider", nullable = false, length = 40)
  private String provider;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "dimension_code", nullable = false, length = 20)
  private BehavioralSnapshotDimension dimension;

  @Column(name = "window_days", nullable = false)
  private Integer windowDays;

  @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
  private String queryText;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "status", nullable = false, length = 20)
  private ExperimentStrategistBehavioralSnapshotStatus status;

  @Column(name = "raw_response", columnDefinition = "LONGTEXT")
  private String rawResponse;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "estimated_cost_usd", nullable = false, precision = 18, scale = 8)
  private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;
}

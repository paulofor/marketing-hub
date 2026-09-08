package com.marketinghub.businessprocesschain.learningcycle.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar cada comando, evidência e transição sem reescrever o histórico. */
@Entity
@Table(name = "learning_sales_cycle_event_v1")
@Getter
@Setter
public class LearningSalesCycleEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "request_key", nullable = false, length = 36)
  private String requestKey;

  @Column(name = "request_json", nullable = false, columnDefinition = "LONGTEXT")
  private String requestJson;

  @Column(name = "revision", nullable = false)
  private long revision;

  @Column(name = "from_stage", nullable = false, length = 32)
  private String fromStage;

  @Column(name = "to_stage", nullable = false, length = 32)
  private String toStage;

  @Column(name = "action", nullable = false, length = 32)
  private String action;

  @Column(name = "operator_name", nullable = false, length = 160)
  private String operatorName;

  @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column(name = "evidence_reference", nullable = false, length = 1200)
  private String evidenceReference;

  @Column(name = "evidence_json", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

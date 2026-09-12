package com.marketinghub.businessprocess.automation.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar decisões e disparos auditáveis da execução automática. */
@Entity
@Table(name = "product_process_run_event_v1")
@Getter
@Setter
public class ProcessRunEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "event_type", nullable = false, length = 40)
  private String eventType;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "activity_id", length = 100)
  private String activityId;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "action_key", length = 64)
  private String actionKey;

  @Column(name = "details_json", nullable = false, columnDefinition = "LONGTEXT")
  private String detailsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

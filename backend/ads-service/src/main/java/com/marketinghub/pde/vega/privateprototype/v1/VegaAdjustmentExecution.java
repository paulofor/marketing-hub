package com.marketinghub.pde.vega.privateprototype.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: persistir cada tentativa de geração com entrada, saída e auditoria próprias.
 */
@Entity
@Table(name = "vega_adjustment_execution_v1")
@Getter
@Setter
public class VegaAdjustmentExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "session_id", nullable = false, length = 36)
  private String sessionId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "request_json", columnDefinition = "LONGTEXT")
  private String requestJson;

  @Column(name = "response_json", columnDefinition = "LONGTEXT")
  private String responseJson;

  @Column(name = "result_json", columnDefinition = "LONGTEXT")
  private String resultJson;

  @Column(name = "error", columnDefinition = "LONGTEXT")
  private String error;

  @Column(name = "model", length = 160)
  private String model;

  @Column(name = "input_tokens")
  private Long inputTokens;

  @Column(name = "output_tokens")
  private Long outputTokens;

  @Column(name = "cost_usd", precision = 16, scale = 8)
  private BigDecimal costUsd;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "lease_until")
  private Instant leaseUntil;
}

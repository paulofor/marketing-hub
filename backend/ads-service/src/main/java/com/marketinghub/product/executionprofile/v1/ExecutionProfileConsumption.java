package com.marketinghub.product.executionprofile.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: auditar reservas e custos sem liberar falha cobrada ou custo desconhecido. */
@Entity
@Table(name = "product_execution_consumption_v1")
@Getter
@Setter
public class ExecutionProfileConsumption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "binding_id", nullable = false)
  private Long bindingId;

  @Column(name = "operation_key", nullable = false, length = 100)
  private String operationKey;

  @Column(name = "usage_key", nullable = false, length = 100)
  private String usageKey;

  @Column(name = "input_hash", nullable = false, length = 64)
  private String inputHash;

  @Column(name = "units", nullable = false)
  private int units;

  @Column(name = "reserved_brl", nullable = false, precision = 18, scale = 6)
  private BigDecimal reservedBrl;

  @Column(name = "actual_brl", precision = 18, scale = 6)
  private BigDecimal actualBrl;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "evidence", columnDefinition = "TEXT")
  private String evidence;

  @Column(name = "test_data", nullable = false)
  private boolean testData;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "finished_at")
  private Instant finishedAt;
}

package com.marketinghub.businessprocesschain.learningcycle.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar uma reserva de consumo no teto integral do recibo original. */
@Entity
@Table(name = "single_video_spend_v1")
@Getter
@Setter
public class SingleVideoSpend {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "receipt_id", nullable = false)
  private Long receiptId;

  @Column(name = "commercial_cycle_id", nullable = false)
  private Long commercialCycleId;

  @Column(name = "video_project_id", nullable = false)
  private Long videoProjectId;

  @Column(name = "operation_key", nullable = false, length = 160)
  private String operationKey;

  @Column(name = "request_sha256", nullable = false, length = 64)
  private String requestSha256;

  @Column(name = "request_json", nullable = false, columnDefinition = "LONGTEXT")
  private String requestJson;

  @Column(name = "provider", nullable = false, length = 32)
  private String provider;

  @Column(name = "model_code", nullable = false, length = 128)
  private String modelCode;

  @Column(name = "max_cost_usd", nullable = false, precision = 18, scale = 8)
  private BigDecimal maxCostUsd;

  @Column(name = "settled_cost_usd", precision = 18, scale = 8)
  private BigDecimal settledCostUsd;

  @Column(name = "state", nullable = false, length = 24)
  private String state;

  @Column(name = "provider_reference", length = 160)
  private String providerReference;

  @Column(name = "raw_response", columnDefinition = "LONGTEXT")
  private String rawResponse;

  @Column(name = "pricing_evidence_json", nullable = false, columnDefinition = "LONGTEXT")
  private String pricingEvidenceJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

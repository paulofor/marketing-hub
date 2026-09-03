package com.marketinghub.salesvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: auditar o snapshot oficial e o dry run de um ciclo de vídeo. */
@Entity
@Table(name = "video_provider_preflight")
@Getter
@Setter
public class VideoProviderPreflight {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_production_cycle_id", nullable = false, unique = true)
  private Long videoProductionCycleId;

  @Column(name = "provider_account_id", nullable = false)
  private Long providerAccountId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "production_profile", nullable = false, length = 32)
  private String productionProfile;

  @Column(name = "router_config_id", length = 80)
  private String routerConfigId;

  @Column(name = "payload_sha256", length = 64)
  private String payloadSha256;

  @Column(name = "execution_requests_json", columnDefinition = "LONGTEXT")
  private String executionRequestsJson;

  @Column(name = "organization_snapshot_json", columnDefinition = "LONGTEXT")
  private String organizationSnapshotJson;

  @Column(name = "routing_response_json", columnDefinition = "LONGTEXT")
  private String routingResponseJson;

  @Column(name = "selected_routes_json", columnDefinition = "LONGTEXT")
  private String selectedRoutesJson;

  @Column(name = "estimated_credits", precision = 14, scale = 4)
  private BigDecimal estimatedCredits;

  @Column(name = "estimated_cost_usd", precision = 12, scale = 6)
  private BigDecimal estimatedCostUsd;

  @Column(name = "official_balance_credits", precision = 14, scale = 4)
  private BigDecimal officialBalanceCredits;

  @Column(name = "reserved_credits_snapshot", precision = 14, scale = 4)
  private BigDecimal reservedCreditsSnapshot;

  @Column(name = "available_credits_snapshot", precision = 14, scale = 4)
  private BigDecimal availableCreditsSnapshot;

  @Column(name = "max_monthly_credit_spend")
  private Long maxMonthlyCreditSpend;

  @Column(name = "quota_snapshot_json", columnDefinition = "LONGTEXT")
  private String quotaSnapshotJson;

  @Column(name = "failure_code", length = 80)
  private String failureCode;

  @Column(name = "failure_detail", columnDefinition = "LONGTEXT")
  private String failureDetail;

  @Column(name = "source_url", nullable = false, length = 500)
  private String sourceUrl;

  @Column(name = "observed_at")
  private Instant observedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

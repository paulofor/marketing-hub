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

/** Responsabilidade: persistir saldo, reservas e limites oficiais de uma conta agregadora. */
@Entity
@Table(name = "video_provider_account")
@Getter
@Setter
public class VideoProviderAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregator_name", nullable = false, length = 120)
  private String aggregatorName;

  @Column(name = "account_key", nullable = false, unique = true, length = 80)
  private String accountKey;

  @Column(name = "display_name", nullable = false, length = 191)
  private String displayName;

  @Column(name = "credit_unit_usd", nullable = false, precision = 12, scale = 6)
  private BigDecimal creditUnitUsd;

  @Column(name = "official_balance_credits", precision = 14, scale = 4)
  private BigDecimal officialBalanceCredits;

  @Column(name = "reserved_credits", nullable = false, precision = 14, scale = 4)
  private BigDecimal reservedCredits = BigDecimal.ZERO;

  @Column(name = "max_monthly_credit_spend")
  private Long maxMonthlyCreditSpend;

  @Column(name = "quota_snapshot_json", columnDefinition = "LONGTEXT")
  private String quotaSnapshotJson;

  @Column(name = "usage_snapshot_json", columnDefinition = "LONGTEXT")
  private String usageSnapshotJson;

  @Column(name = "snapshot_status", nullable = false, length = 32)
  private String snapshotStatus = "UNKNOWN";

  @Column(name = "snapshot_observed_at")
  private Instant snapshotObservedAt;

  @Column(name = "snapshot_expires_at")
  private Instant snapshotExpiresAt;

  @Column(name = "source_url", nullable = false, length = 500)
  private String sourceUrl;

  @Column(name = "recharge_url", length = 500)
  private String rechargeUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

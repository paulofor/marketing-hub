package com.marketinghub.facebookads.resumption;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Persiste autorização, execução e evidência da retomada de uma campanha existente. */
@Entity
@Table(name = "facebook_campaign_resumption")
@Getter
@Setter
public class FacebookCampaignResumption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "campaign_id", nullable = false, length = 36)
  private String campaignId;

  @Column(name = "ad_set_id", nullable = false, length = 36)
  private String adSetId;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "total_limit", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalLimit;

  @Column(name = "daily_budget", precision = 10, scale = 2)
  private BigDecimal dailyBudget;

  @Column(name = "previous_limit", precision = 10, scale = 2)
  private BigDecimal previousLimit;

  @Column(name = "previous_start_date")
  private LocalDate previousStartDate;

  @Column(name = "previous_end_date")
  private LocalDate previousEndDate;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "zero_result_spend_limit", precision = 10, scale = 2)
  private BigDecimal zeroResultSpendLimit;

  @Column(name = "zero_purchase_spend_limit", precision = 10, scale = 2)
  private BigDecimal zeroPurchaseSpendLimit;

  @Column(name = "purchase_stop_count")
  private Integer purchaseStopCount;

  @Column(name = "reason", nullable = false, length = 1024)
  private String reason;

  @Column(name = "destination_url", nullable = false, length = 2048)
  private String destinationUrl;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "lease_token", length = 36)
  private String leaseToken;

  @Column(name = "lease_until")
  private Instant leaseUntil;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "error", columnDefinition = "TEXT")
  private String error;

  @Column(name = "evidence", columnDefinition = "LONGTEXT")
  private String evidence;
}

package com.marketinghub.salesvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Responsabilidade: persistir a curadoria e os gates de um modelo de vídeo homologável. */
@Entity
@Table(name = "sales_video_provider_model")
@Data
@NoArgsConstructor
public class SalesVideoProviderModel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 80)
  private String code;

  @Column(name = "display_name", nullable = false, length = 140)
  private String displayName;

  @Column(name = "manufacturer_name", nullable = false, length = 120)
  private String manufacturerName;

  @Column(name = "aggregator_name", nullable = false, length = 120)
  private String aggregatorName;

  @Column(name = "provider_account_key", length = 80)
  private String providerAccountKey;

  @Column(name = "route_key", nullable = false, length = 120)
  private String routeKey;

  @Column(name = "provider_name", nullable = false, unique = true, length = 80)
  private String providerName;

  @Column(name = "provider_family", nullable = false, length = 40)
  private String providerFamily;

  @Column(name = "adapter_key", nullable = false, length = 80)
  private String adapterKey;

  @Column(name = "external_model_id", nullable = false, length = 120)
  private String externalModelId;

  @Column(name = "recommended_use", nullable = false, length = 500)
  private String recommendedUse;

  @Column(name = "lifecycle_status", nullable = false, length = 24)
  private String lifecycleStatus;

  @Column(name = "clip_duration_seconds", nullable = false)
  private Integer clipDurationSeconds;

  @Column(name = "max_direct_duration_seconds", nullable = false)
  private Integer maxDirectDurationSeconds;

  @Column(name = "supports_hero_video", nullable = false)
  private boolean supportsHeroVideo;

  @Column(name = "supports_scene_assembly", nullable = false)
  private boolean supportsSceneAssembly;

  @Column(name = "requires_source_image", nullable = false)
  private boolean requiresSourceImage;

  @Column(name = "credits_url", length = 500)
  private String creditsUrl;

  @Column(name = "documentation_url", nullable = false, length = 500)
  private String documentationUrl;

  @Column(name = "adapter_verified", nullable = false)
  private boolean adapterVerified;

  @Column(name = "pricing_verified", nullable = false)
  private boolean pricingVerified;

  @Column(name = "pricing_amount_usd", precision = 12, scale = 6)
  private BigDecimal pricingAmountUsd;

  @Column(name = "pricing_unit", length = 24)
  private String pricingUnit;

  @Column(name = "pricing_quantity", precision = 12, scale = 4)
  private BigDecimal pricingQuantity;

  @Column(name = "pricing_resolution", length = 40)
  private String pricingResolution;

  @Column(name = "pricing_includes_audio")
  private Boolean pricingIncludesAudio;

  @Column(name = "pricing_source_url", length = 500)
  private String pricingSourceUrl;

  @Column(name = "pricing_observed_at")
  private Instant pricingObservedAt;

  @Column(name = "pricing_research_status", nullable = false, length = 24)
  private String pricingResearchStatus = "PENDING";

  @Column(name = "pricing_research_notes", length = 1000)
  private String pricingResearchNotes;

  @Column(name = "pricing_research_raw_response", columnDefinition = "LONGTEXT")
  private String pricingResearchRawResponse;

  @Column(name = "pricing_research_model", length = 120)
  private String pricingResearchModel;

  @Column(name = "commercial_license_verified", nullable = false)
  private boolean commercialLicenseVerified;

  @Column(name = "quality_gate_verified", nullable = false)
  private boolean qualityGateVerified;

  @Column(name = "notes", length = 1000)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

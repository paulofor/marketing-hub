package com.marketinghub.salesvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

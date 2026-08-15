package com.marketinghub.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Responsabilidade: vincular uma referência audiovisual reutilizável e governada ao plano
 * comercial.
 */
@Entity
@Table(name = "commercial_plan_visual_asset")
@Getter
@Setter
@NoArgsConstructor
public class CommercialPlanVisualAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @Column(name = "asset_url", nullable = false, length = 2048)
  private String assetUrl;

  @Column(name = "media_type", nullable = false, length = 16)
  private String mediaType;

  @Column(name = "label", nullable = false, length = 191)
  private String label;

  @Column(name = "purpose", nullable = false, length = 64)
  private String purpose;

  @Column(name = "origin", nullable = false, length = 191)
  private String origin;

  @Column(name = "rights_statement", nullable = false, length = 512)
  private String rightsStatement;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private CommercialPlanVisualAssetStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

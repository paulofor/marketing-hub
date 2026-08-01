package com.marketinghub.salesvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Projeto editável que concentra a definição criativa e comercial de um vídeo. */
@Entity
@Table(name = "video_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Builder.Default
  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId = "default";

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "experiment_id")
  private Long experimentId;

  @Column(name = "sales_video_profile_id")
  private Long salesVideoProfileId;

  @Column(name = "campaign_key", length = 191)
  private String campaignKey;

  @Builder.Default
  @Column(name = "video_category", nullable = false, length = 64)
  private String videoCategory = "LONG_FORM";

  @Column(name = "context_type", nullable = false, length = 64)
  private String contextType;

  @Column(name = "production_mode", nullable = false, length = 64)
  private String productionMode;

  @Column(name = "target_channel", nullable = false, length = 64)
  private String targetChannel;

  @Column(name = "format", nullable = false, length = 64)
  private String format;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "objective", nullable = false, length = 1024)
  private String objective;

  @Column(name = "story_text", columnDefinition = "LONGTEXT")
  private String storyText;

  @Column(name = "funnel_stage", length = 64)
  private String funnelStage;

  @Column(name = "primary_metric", length = 191)
  private String primaryMetric;

  @Column(name = "hook_text", length = 1024)
  private String hookText;

  @Column(name = "script_text", columnDefinition = "LONGTEXT")
  private String scriptText;

  @Column(name = "scene_plan", columnDefinition = "LONGTEXT")
  private String scenePlan;

  @Column(name = "visual_references", columnDefinition = "LONGTEXT")
  private String visualReferences;

  @Column(name = "character_bible", columnDefinition = "LONGTEXT")
  private String characterBible;

  @Column(name = "environment_bible", columnDefinition = "LONGTEXT")
  private String environmentBible;

  @Column(name = "object_bible", columnDefinition = "LONGTEXT")
  private String objectBible;

  @Column(name = "visual_style_guide", columnDefinition = "LONGTEXT")
  private String visualStyleGuide;

  @Column(name = "image_generation_plan", columnDefinition = "LONGTEXT")
  private String imageGenerationPlan;

  @Column(name = "continuity_rules", columnDefinition = "LONGTEXT")
  private String continuityRules;

  @Column(name = "voiceover_plan", columnDefinition = "LONGTEXT")
  private String voiceoverPlan;

  @Column(name = "soundtrack_plan", columnDefinition = "LONGTEXT")
  private String soundtrackPlan;

  @Column(name = "caption_plan", columnDefinition = "LONGTEXT")
  private String captionPlan;

  @Column(name = "cta_text", length = 1024)
  private String ctaText;

  @Column(name = "target_duration_seconds")
  private Integer targetDurationSeconds;

  @Column(name = "provider_plan", columnDefinition = "LONGTEXT")
  private String providerPlan;

  @Column(name = "editing_notes", columnDefinition = "LONGTEXT")
  private String editingNotes;

  @Column(name = "quality_gate", columnDefinition = "LONGTEXT")
  private String qualityGate;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 64)
  private VideoProjectStatus status = VideoProjectStatus.DRAFT;

  @Column(name = "created_by", length = 191)
  private String createdBy;

  @Column(name = "updated_by", length = 191)
  private String updatedBy;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}

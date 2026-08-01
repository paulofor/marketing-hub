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

/** Vídeo externo de sucesso enviado pelo usuário para análise e aprendizado comercial. */
@Entity
@Table(name = "video_reference")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoReference {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Builder.Default
  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId = "default";

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "source_url", nullable = false, length = 2048)
  private String sourceUrl;

  @Column(name = "source_platform", length = 64)
  private String sourcePlatform;

  @Column(name = "niche", length = 191)
  private String niche;

  @Column(name = "funnel_stage", length = 64)
  private String funnelStage;

  @Column(name = "primary_learning_goal", nullable = false, length = 1024)
  private String primaryLearningGoal;

  @Column(name = "success_evidence", columnDefinition = "LONGTEXT")
  private String successEvidence;

  @Column(name = "analysis_notes", columnDefinition = "LONGTEXT")
  private String analysisNotes;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 64)
  private VideoReferenceStatus status = VideoReferenceStatus.QUEUED;

  @Column(name = "created_by", length = 191)
  private String createdBy;

  @Column(name = "analyzed_at")
  private Instant analyzedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}

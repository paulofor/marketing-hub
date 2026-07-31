package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Playbook comercial por perfil para orientar objeções, CTAs e briefings cinematográficos. */
@Entity
@Table(name = "sales_video_commercial_playbook")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoCommercialPlaybook {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "profile_id", nullable = false)
  @ToString.Exclude
  private SalesVideoProfile profile;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "niche_key", nullable = false, length = 120)
  private String nicheKey;

  @Column(name = "variant_key", nullable = false, length = 120)
  private String variantKey;

  @Lob
  @Column(name = "objection_text", nullable = false)
  private String objectionText;

  @Lob
  @Column(name = "cta_text", nullable = false)
  private String ctaText;

  @Column(name = "funnel_role", length = 80)
  private String funnelRole;

  @Lob
  @Column(name = "promise_to_visualize")
  private String promiseToVisualize;

  @Lob
  @Column(name = "visual_pain")
  private String visualPain;

  @Lob
  @Column(name = "main_scene")
  private String mainScene;

  @Lob
  @Column(name = "subject_description")
  private String subjectDescription;

  @Lob
  @Column(name = "motion_description")
  private String motionDescription;

  @Lob
  @Column(name = "camera_framing")
  private String cameraFraming;

  @Lob
  @Column(name = "lighting_style")
  private String lightingStyle;

  @Column(name = "expected_emotion", length = 120)
  private String expectedEmotion;

  @Lob
  @Column(name = "transition_or_cta")
  private String transitionOrCta;

  @Lob
  @Column(name = "quality_constraints")
  private String qualityConstraints;

  @Lob
  @Column(name = "cinematic_prompt")
  private String cinematicPrompt;

  @Builder.Default
  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "created_by")
  private String createdBy;

  @CreationTimestamp private Instant createdAt;

  @UpdateTimestamp private Instant updatedAt;
}

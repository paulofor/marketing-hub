package com.marketinghub.salesvideo.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/** Responsabilidade: expor playbook comercial e briefing cinematográfico de vídeo. */
@Data
@Builder
public class SalesVideoCommercialPlaybookDto {
  private Long id;
  private Long profileId;
  private String tenantId;
  private String nicheKey;
  private String variantKey;
  private String objectionText;
  private String ctaText;
  private String funnelRole;
  private String promiseToVisualize;
  private String visualPain;
  private String mainScene;
  private String subjectDescription;
  private String motionDescription;
  private String cameraFraming;
  private String lightingStyle;
  private String expectedEmotion;
  private String transitionOrCta;
  private String qualityConstraints;
  private String cinematicPrompt;
  private boolean active;
  private String createdBy;
  private Instant createdAt;
  private Instant updatedAt;
}

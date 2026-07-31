package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Responsabilidade: receber playbook comercial e briefing cinematográfico de vídeo. */
@Data
public class CreateSalesVideoCommercialPlaybookRequest {
  @NotBlank private String nicheKey;

  @NotBlank private String variantKey;

  @NotBlank private String objectionText;

  @NotBlank private String ctaText;

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
  private Boolean active;
  private String createdBy;
}

package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/** Representação de leitura de um criativo. */
@Data
public class CreativeDto {
  private Long id;
  private Long experimentId;
  private String format;
  private String headline;
  private String primaryText;
  private String imageUrl;
  private String videoId;
  private String videoUrl;
  private BigDecimal costUsd;
  private String description;
  private String cta;
  private String destinationUrl;
  private String leadGenFormId;
  private String instagramUserId;
  private String imagePrompt;
  private String imageIntermediatePrompt;
  private CreativeStatus status;
  private String rejectionReason;
  private Instant reviewedAt;
}

package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Solicita acabamento, recuperação da voz preservada ou HLS de um MP4 já produzido. */
@Data
public class RequestSalesVideoPostProductionRequest {
  @NotBlank private String requestedBy;

  private String sourceVideoUrl;

  private String voiceOverScript;

  @NotBlank private String captionText;

  private boolean deliveryOnly;

  @Min(6)
  @Max(60)
  private Integer targetDurationSeconds;
}

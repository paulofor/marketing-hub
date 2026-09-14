package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Solicita acabamento ou somente HLS preservando um MP4 final já produzido. */
@Data
public class RequestSalesVideoPostProductionRequest {
  @NotBlank private String requestedBy;

  private String sourceVideoUrl;

  private String voiceOverScript;

  @NotBlank private String captionText;

  private boolean deliveryOnly;
}

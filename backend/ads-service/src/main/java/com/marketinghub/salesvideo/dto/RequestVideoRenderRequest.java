package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Solicitação para criar um job de renderização. */
@Data
public class RequestVideoRenderRequest {
  @NotBlank private String requestedBy;

  private SalesVideoProviderFamily providerFamily;
  private String providerName;
  private SalesVideoExecutionMode executionMode;
  @Positive private Integer targetDurationSeconds;
  @Positive private Long continuitySourceJobId;
  private String metadataJson;
}

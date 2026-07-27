package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoStatus;
import java.math.BigDecimal;
import lombok.Data;

/** Payload para finalizar um job. */
@Data
public class JobCompletionRequest {
  private SalesVideoStatus status;
  private Long assetId;
  private Long posterAssetId;
  private Long vttAssetId;
  private String providerJobId;
  private String streamPlaybackUrl;
  private String metadataJson;
  private BigDecimal costUsd;
  private String message;
  private String detailsJson;
  private GeneratedScriptResultPayload scriptResult;
}

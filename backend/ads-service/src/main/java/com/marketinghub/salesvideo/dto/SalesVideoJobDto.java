package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

import java.time.Instant;

/**
 * DTO com resumo de um job do pipeline de vídeos.
 */
@Data
public class SalesVideoJobDto {
    private Long id;
    private Long profileId;
    private Long scriptId;
    private String tenantId;
    private SalesVideoProviderFamily providerFamily;
    private String providerName;
    private String providerJobId;
    private SalesVideoJobType jobType;
    private SalesVideoStatus status;
    private Integer retryAttempt;
    private SalesVideoRetryReason retryReason;
    private Long retryOfJobId;
    private String retryNotes;
    private Integer progressPercent;
    private String failureCode;
    private String failureDetail;
    private String requestedBy;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant expiresAt;
    private Long assetId;
    private Long posterAssetId;
    private Long vttAssetId;
    private String metadataJson;
    private Instant createdAt;
    private Instant updatedAt;
}

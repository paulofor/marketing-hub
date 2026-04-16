package com.marketinghub.videomanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesVideoJob(
        Long id,
        Long profileId,
        Long scriptId,
        String tenantId,
        SalesVideoProviderFamily providerFamily,
        String providerName,
        String providerJobId,
        SalesVideoJobType jobType,
        SalesVideoStatus status,
        Integer retryAttempt,
        String retryReason,
        Long retryOfJobId,
        String retryNotes,
        Integer progressPercent,
        String failureCode,
        String failureDetail,
        String requestedBy,
        Instant requestedAt,
        Instant startedAt,
        Instant finishedAt,
        Instant expiresAt,
        Long assetId,
        Long posterAssetId,
        Long vttAssetId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt) {
}

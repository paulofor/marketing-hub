package com.marketinghub.videomanagement.client.payload;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: reportar ao backend o snapshot sanitizado e o dry run do agregador. */
public record ProviderPreflightResultPayload(
        String status,
        String accountKey,
        String routerConfigId,
        String payloadSha256,
        String executionRequestsJson,
        String organizationSnapshotJson,
        String routingResponseJson,
        String selectedRoutesJson,
        BigDecimal estimatedCredits,
        BigDecimal officialBalanceCredits,
        Long maxMonthlyCreditSpend,
        String quotaSnapshotJson,
        String usageSnapshotJson,
        String failureCode,
        String failureDetail,
        String sourceUrl,
        Instant observedAt) {}

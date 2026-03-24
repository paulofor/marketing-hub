package com.marketinghub.videomanagement.job;

import java.time.Instant;

/**
 * Estrutura mínima esperada a partir do backend.
 */
public record VideoJobSummary(
        Long id,
        Long profileId,
        String jobType,
        String providerFamily,
        String providerName,
        String status,
        Integer progressPercent,
        Instant requestedAt
) {
}

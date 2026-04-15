package com.marketinghub.oprm.integration.contract;

import java.util.Map;

public record OprmJobClaimResponse(
        String jobId,
        OprmJobType jobType,
        String occupationSeedRef,
        String correlationId,
        Map<String, Object> parameters,
        String claimedAt,
        String leaseExpiresAt
) {
}

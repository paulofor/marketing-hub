package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmJobType;
import java.util.Map;

public record OprmJobClaimResponseDto(
        String jobId,
        OprmJobType jobType,
        String occupationSeedRef,
        String correlationId,
        Map<String, Object> parameters,
        String claimedAt,
        String leaseExpiresAt
) {
}

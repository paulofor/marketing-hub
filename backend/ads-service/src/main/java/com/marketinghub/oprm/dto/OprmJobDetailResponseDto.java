package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmJobStatus;
import com.marketinghub.oprm.OprmJobType;
import java.util.List;
import java.util.Map;

public record OprmJobDetailResponseDto(
        String jobId,
        OprmJobType jobType,
        OprmJobStatus jobStatus,
        String occupationSeedRef,
        String correlationId,
        int attemptCount,
        String createdAt,
        String claimedAt,
        String startedAt,
        String finishedAt,
        Map<String, Object> parameters,
        List<String> inputRefs,
        String errorCode,
        String errorMessage
) {
}

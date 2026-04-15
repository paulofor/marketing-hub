package com.marketinghub.oprm.integration.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmJobStatusUpdateRequest(
        @NotBlank String workerId,
        @NotNull OprmJobStatus status,
        @NotBlank String occurredAt,
        String phase,
        String message,
        String errorCode,
        String errorMessage,
        Map<String, Object> metrics
) {
}

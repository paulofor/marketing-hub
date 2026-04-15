package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmJobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmJobStatusUpdateRequestDto(
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

package com.marketinghub.oprm.integration.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OprmHeartbeatRequest(
        @NotBlank String workerId,
        @NotBlank String workerVersion,
        @NotBlank String contractVersion,
        @NotBlank String sentAt,
        @NotNull Map<String, Object> health,
        @NotNull Map<String, Object> counters
) {
}

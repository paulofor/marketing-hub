package com.marketinghub.worker.geralanding.deliverables.dto;

import java.time.Instant;
import java.util.UUID;

/** Responsabilidade: representar os dados do job OpenAI da etapa deliverables. */
public record RecordJobDto(
        UUID id,
        Long experimentId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}

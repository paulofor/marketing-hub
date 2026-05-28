package com.marketinghub.worker.geralanding.presetdesign.dto;

import java.time.Instant;
import java.util.UUID;

/** Responsabilidade: representar os dados do job OpenAI da etapa presetdesign. */
public record GeraLandingJobDto(
        UUID id,
        Long experimentId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}

package com.marketinghub.worker.geralanding;

import java.time.Instant;
import java.util.UUID;

/** Responsabilidade: representar os dados do job OpenAI do fluxo geral do GeraLanding. */
public record RecordJobDto(
        UUID id,
        Long experimentId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}

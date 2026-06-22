package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.cancelJob;

import java.time.Instant;

/** Retorna o resultado do cancelamento manual de um job preso do NichoCNAE v2. */
public record CandidateGeneratorCancelJobResponse(
        String jobId,
        String cnaeCode,
        int canceledExecutions,
        String status,
        String message,
        Instant updatedAt) {}

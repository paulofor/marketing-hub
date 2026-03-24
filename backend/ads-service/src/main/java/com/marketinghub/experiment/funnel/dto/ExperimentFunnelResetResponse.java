package com.marketinghub.experiment.funnel.dto;

import java.time.Instant;

/**
 * Resposta retornada após um reset manual do funil do experimento.
 */
public record ExperimentFunnelResetResponse(Instant resetAt) {
}

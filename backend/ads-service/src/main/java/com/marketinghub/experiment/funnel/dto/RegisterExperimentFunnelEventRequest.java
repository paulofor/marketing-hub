package com.marketinghub.experiment.funnel.dto;

import com.marketinghub.experiment.funnel.ExperimentFunnelStage;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload para registrar manualmente uma interação no funil do experimento.
 */
public record RegisterExperimentFunnelEventRequest(
        ExperimentFunnelStage stage,
        UUID leadId,
        String source,
        String payload,
        Instant occurredAt
) {
}

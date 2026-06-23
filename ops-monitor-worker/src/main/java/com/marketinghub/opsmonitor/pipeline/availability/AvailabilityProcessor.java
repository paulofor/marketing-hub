package com.marketinghub.opsmonitor.pipeline.availability;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import com.marketinghub.opsmonitor.pipeline.StageProcessor;

/** Consolida o estado operacional do módulo a partir das verificações recentes. */
public class AvailabilityProcessor implements StageProcessor<AvailabilityInput, AvailabilityOutput> {

    /** Classifica disponibilidade como ONLINE, DEGRADED, OFFLINE ou UNKNOWN. */
    @Override
    public AvailabilityOutput process(StageContext context, AvailabilityInput input) {
        int threshold = input.offlineThresholdFailures() <= 0 ? 3 : input.offlineThresholdFailures();
        if (input.consecutiveFailures() >= threshold) {
            return new AvailabilityOutput(input.moduleCode(), "OFFLINE", "Falhas consecutivas acima do limite");
        }
        if (!input.lastCheckSuccessful() || input.responseTimeMs() > 5000) {
            return new AvailabilityOutput(input.moduleCode(), "DEGRADED", "Falha isolada ou resposta lenta");
        }
        return new AvailabilityOutput(input.moduleCode(), "ONLINE", "Última verificação com sucesso");
    }
}

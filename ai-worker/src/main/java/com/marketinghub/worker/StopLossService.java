package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Avalia regras de stop-loss para conjuntos de anúncio.
 */
@Service
public class StopLossService {
    private static final Logger log = LoggerFactory.getLogger(StopLossService.class);

    private final MetricPresetCache presetCache;

    public StopLossService(MetricPresetCache presetCache) {
        this.presetCache = presetCache;
    }

    public boolean shouldPause(double spend, int clicks, double cpa, double kpiTarget, int sampleSize) {
        int checkpoint = presetCache.checkpointForSampleSize(sampleSize);
        boolean result = clicks >= checkpoint && spend > 0 && cpa > kpiTarget * 2;
        if (result) {
            log.info("Stop-loss acionado: CPA {} > {}", cpa, kpiTarget * 2);
        }
        return result;
    }
}

package com.marketinghub.worker;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory cache for metric preset limits.
 */
@Component
public class MetricPresetCache {
    private final Map<Integer, Integer> checkpoints = new HashMap<>();

    public MetricPresetCache() {
        checkpoints.put(300, 50);
        checkpoints.put(150, 30);
        checkpoints.put(100, 25);
    }

    /**
     * Returns checkpoint clicks for given sample size.
     */
    public int checkpointForSampleSize(int sampleSize) {
        if (sampleSize >= 300) {
            return checkpoints.get(300);
        }
        if (sampleSize >= 150) {
            return checkpoints.get(150);
        }
        return checkpoints.get(100);
    }
}

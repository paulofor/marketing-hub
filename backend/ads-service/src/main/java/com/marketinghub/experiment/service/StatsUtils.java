package com.marketinghub.experiment.service;

/** Utility methods for experiment statistics. */
public final class StatsUtils {
    private StatsUtils() {}

    public static record ConfidenceInterval(double lower, double upper) {}

    /**
     * 95% confidence interval for conversion rate using normal approximation.
     */
    public static ConfidenceInterval ci95(int successes, int trials) {
        if (trials == 0) return new ConfidenceInterval(0, 0);
        double p = successes / (double) trials;
        double z = 1.96;
        double se = Math.sqrt(p * (1 - p) / trials);
        double lower = Math.max(0, p - z * se);
        double upper = Math.min(1, p + z * se);
        return new ConfidenceInterval(lower, upper);
    }
}

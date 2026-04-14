package com.marketinghub.experiment.funnel.dto;

public record FunnelThresholdCheckDto(
        Double minAcceptableRate,
        int attemptsFor95Confidence,
        Double upper95RateIfZero,
        boolean statisticallyFailed,
        boolean attemptsTargetReached
) {
}

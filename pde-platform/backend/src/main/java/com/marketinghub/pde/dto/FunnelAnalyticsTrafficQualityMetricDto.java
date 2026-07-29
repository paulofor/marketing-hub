package com.marketinghub.pde.dto;

/** Resume sessões PDE por qualidade de tráfego para separar humanos de robôs e QA. */
public record FunnelAnalyticsTrafficQualityMetricDto(
        String trafficQuality,
        String label,
        long sessions,
        long events,
        double percentage
) {}

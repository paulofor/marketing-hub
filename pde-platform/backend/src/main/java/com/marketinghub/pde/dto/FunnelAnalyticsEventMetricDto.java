package com.marketinghub.pde.dto;

/** Representa a contagem agregada de um evento do funil PED/MUSA. */
public record FunnelAnalyticsEventMetricDto(
        String eventType,
        long total
) {}

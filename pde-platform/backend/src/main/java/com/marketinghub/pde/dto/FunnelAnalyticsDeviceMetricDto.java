package com.marketinghub.pde.dto;

/** Resume sessões PDE por tipo de dispositivo capturado no navegador. */
public record FunnelAnalyticsDeviceMetricDto(
        String deviceType,
        String label,
        long sessions,
        double percentage
) {}

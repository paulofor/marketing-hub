package com.marketinghub.pde.dto;

/** Resume sessões PDE por resolução de tela capturada no navegador. */
public record FunnelAnalyticsScreenSizeMetricDto(
        String screenSize,
        String label,
        Integer width,
        Integer height,
        long sessions,
        double percentage
) {}

package com.marketinghub.experiment.monitoring.dto;

/** Resume sessões PDE por resolução de tela capturada no navegador. */
public record PostDeployPdeScreenSizeDto(
        String screenSize,
        String label,
        Integer width,
        Integer height,
        long sessions,
        double percentage
) {}

package com.marketinghub.experiment.monitoring.dto;

/** Resume sessões PDE por tipo de dispositivo capturado no navegador. */
public record PostDeployPdeDeviceDto(
    String deviceType, String label, long sessions, double percentage) {}

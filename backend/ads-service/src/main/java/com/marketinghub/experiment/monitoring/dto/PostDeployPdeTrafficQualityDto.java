package com.marketinghub.experiment.monitoring.dto;

/** Resume a qualidade do tráfego PDE para separar KPIs comerciais de auditoria técnica. */
public record PostDeployPdeTrafficQualityDto(
    String trafficQuality, String label, long sessions, long events, double percentage) {}

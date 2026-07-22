package com.marketinghub.pde.dto;

/** Expõe alerta operacional que pode invalidar leitura de funil no pós-deploy. */
public record DeployOperationalAlertResponse(
        String severity,
        String type,
        String funnelStage,
        String endpoint,
        String message,
        String evidence,
        long recentFailures,
        String lastSeenAt,
        String recommendedAction
) {}

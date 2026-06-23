package com.marketinghub.opsmonitor.service.listPendingChecks;

/** Dados que orientam o worker na próxima verificação de saúde de um módulo. */
public record PendingModuleCheckResponse(String moduleCode, String name, String type, String baseUrl,
        String healthPath, String logPath, String criticality, Integer offlineThresholdSeconds) {}

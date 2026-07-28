package com.marketinghub.pdemonitor.db;

/** Representa um PDE crítico cadastrado para verificação operacional direta. */
public record PdeMonitoredModule(
        long id,
        String code,
        String name,
        String baseUrl,
        String healthPath,
        String monitoringUrl,
        int offlineThresholdSeconds) {}

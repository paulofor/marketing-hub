package com.marketinghub.opsmonitor.service.moduleRegistry;

import java.time.Instant;

/** Contrato administrativo com os dados completos do módulo monitorado. */
public record OpsMonitoredModuleResponse(
    Long id,
    String code,
    String name,
    String type,
    String baseUrl,
    String healthPath,
    String logPath,
    String publishedVersion,
    String productUrl,
    String monitoringUrl,
    String containerImageVersion,
    Boolean enabled,
    String criticality,
    Integer offlineThresholdSeconds,
    Instant createdAt,
    Instant updatedAt) {}

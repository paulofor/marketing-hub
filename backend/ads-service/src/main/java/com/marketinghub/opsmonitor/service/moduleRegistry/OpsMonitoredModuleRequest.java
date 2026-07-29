package com.marketinghub.opsmonitor.service.moduleRegistry;

/** Contrato administrativo para criar ou atualizar um módulo monitorado. */
public record OpsMonitoredModuleRequest(
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
    Integer offlineThresholdSeconds) {}

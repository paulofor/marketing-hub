package com.marketinghub.opsmonitor.pipeline.healthcheck;

/** Representa uma pendência de verificação entregue pelo backend ao worker. */
public record PendingModuleCheck(String moduleCode, String name, String type, String baseUrl, String healthPath,
        String logPath, String criticality, Integer offlineThresholdSeconds) {}

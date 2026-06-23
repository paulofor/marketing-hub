package com.marketinghub.opsmonitor.pipeline.healthcheck;

/** Representa o resultado da chamada de saúde feita para um módulo. */
public record HealthCheckOutput(String moduleCode, String status, Integer httpStatus, long responseTimeMs, String rawPayload, String errorMessage) {}

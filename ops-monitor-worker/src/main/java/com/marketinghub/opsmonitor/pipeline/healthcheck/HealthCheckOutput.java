package com.marketinghub.opsmonitor.pipeline.healthcheck;

import java.time.Instant;

/** Representa o resultado da chamada de saúde feita para um módulo. */
public record HealthCheckOutput(String moduleCode, Instant checkedAt, String status, Integer httpStatus, long responseTimeMs, String rawPayload, String errorMessage) {}

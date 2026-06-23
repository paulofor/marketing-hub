package com.marketinghub.opsmonitor.pipeline.healthcheck;

import java.time.Duration;

/** Representa a entrada necessária para verificar a saúde de um módulo. */
public record HealthCheckInput(String moduleCode, String url, Duration timeout) {}

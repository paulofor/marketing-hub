package com.marketinghub.opsmonitor.pipeline.healthcheck;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Guarda configurações da etapa de health check. */
@ConfigurationProperties(prefix = "ops-monitor.health-check")
public record HealthCheckProperties(Duration defaultTimeout) {}

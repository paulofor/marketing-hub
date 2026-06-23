package com.marketinghub.opsmonitor.pipeline.availability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Guarda limites de classificação da etapa de disponibilidade. */
@ConfigurationProperties(prefix = "ops-monitor.availability")
public record AvailabilityProperties(long degradedResponseTimeMs, int offlineFailureThreshold) {}

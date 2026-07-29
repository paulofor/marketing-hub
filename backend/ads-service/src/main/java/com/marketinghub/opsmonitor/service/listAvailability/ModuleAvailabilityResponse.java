package com.marketinghub.opsmonitor.service.listAvailability;

import java.time.Instant;

/** Status atual de disponibilidade de um módulo monitorado. */
public record ModuleAvailabilityResponse(
    String moduleCode,
    String name,
    String type,
    String criticality,
    String publishedVersion,
    String productUrl,
    String monitoringUrl,
    String containerImageVersion,
    String status,
    Instant lastCheckedAt,
    Long lastResponseTimeMs,
    String lastError,
    String attemptedUrl,
    Long lastCheckAgeSeconds,
    boolean heartbeatStale,
    String statusReason) {}

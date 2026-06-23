package com.marketinghub.opsmonitor.service.listIncidents;

import java.time.Instant;

/** Incidente operacional apresentado nas telas administrativas. */
public record ModuleIncidentResponse(Long id, String moduleCode, String moduleName, String status, String severity,
        Instant startedAt, Instant endedAt, Long durationSeconds, String summary, String rootSignal, String lastError) {}

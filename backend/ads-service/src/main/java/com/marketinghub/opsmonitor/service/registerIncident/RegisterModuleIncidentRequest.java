package com.marketinghub.opsmonitor.service.registerIncident;

import java.time.Instant;

/** Payload enviado pelo worker para registrar um incidente operacional. */
public record RegisterModuleIncidentRequest(String status, String severity, Instant startedAt, Instant endedAt,
        Long durationSeconds, String summary, String rootSignal, String lastError) {}

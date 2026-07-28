package com.marketinghub.pdemonitor.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Guarda limites operacionais usados pelo monitor dedicado de PDEs. */
@ConfigurationProperties(prefix = "pde-monitor")
public record PdeMonitorProperties(Http http, Incident incident) {

    /** Resolve o timeout HTTP com valor padrão seguro. */
    public Duration httpTimeout() {
        return http == null || http.timeout() == null ? Duration.ofSeconds(5) : http.timeout();
    }

    /** Resolve o limite de degradação por latência. */
    public long degradedResponseTimeMs() {
        return http == null || http.degradedResponseTimeMs() <= 0 ? 3000 : http.degradedResponseTimeMs();
    }

    /** Resolve a severidade usada em incidentes abertos pelo monitor. */
    public String incidentSeverity() {
        return incident == null || incident.severity() == null || incident.severity().isBlank()
                ? "CRITICAL"
                : incident.severity();
    }

    /** Configura limites das chamadas HTTP feitas contra os PDEs. */
    public record Http(Duration timeout, long degradedResponseTimeMs) {}

    /** Configura a severidade dos incidentes operacionais. */
    public record Incident(String severity) {}
}

package com.marketinghub.pdemonitor.health;

import com.marketinghub.pdemonitor.config.PdeMonitorProperties;
import com.marketinghub.pdemonitor.db.PdeMonitoredModule;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Executa chamadas HTTP públicas para validar disponibilidade real dos PDEs. */
@Component
public class PdeHealthChecker {
    private final WebClient webClient;
    private final PdeMonitorProperties properties;

    /** Recebe o cliente HTTP e os limites operacionais de verificação. */
    public PdeHealthChecker(WebClient.Builder builder, PdeMonitorProperties properties) {
        this.webClient = builder.build();
        this.properties = properties;
    }

    /** Verifica a URL operacional do PDE e classifica o resultado. */
    public PdeHealthResult check(PdeMonitoredModule module) {
        long started = System.nanoTime();
        Instant checkedAt = Instant.now();
        String url = targetUrl(module);
        try {
            var entity =
                    webClient
                            .get()
                            .uri(url)
                            .exchangeToMono(response -> response.toEntity(String.class))
                            .timeout(properties.httpTimeout())
                            .block();
            long elapsed = elapsedMs(started);
            int httpStatus = entity.getStatusCode().value();
            String status = statusFrom(httpStatus, elapsed);
            return new PdeHealthResult(checkedAt, status, httpStatus, elapsed, entity.getBody(), null);
        } catch (RuntimeException ex) {
            return new PdeHealthResult(
                    checkedAt,
                    "OFFLINE",
                    null,
                    elapsedMs(started),
                    null,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /** Resolve a URL final usando monitoring_url como prioridade. */
    public String targetUrl(PdeMonitoredModule module) {
        if (module.monitoringUrl() != null && !module.monitoringUrl().isBlank()) {
            return module.monitoringUrl();
        }
        String baseUrl = stripTrailingSlash(module.baseUrl());
        String healthPath =
                module.healthPath() == null || module.healthPath().isBlank() ? "/healthz" : module.healthPath();
        return baseUrl + (healthPath.startsWith("/") ? healthPath : "/" + healthPath);
    }

    /** Classifica status HTTP e latência como disponibilidade operacional. */
    private String statusFrom(int httpStatus, long elapsedMs) {
        if (httpStatus < 200 || httpStatus >= 300) {
            return "DEGRADED";
        }
        return elapsedMs > properties.degradedResponseTimeMs() ? "DEGRADED" : "ONLINE";
    }

    /** Calcula o tempo decorrido em milissegundos. */
    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    /** Remove barra final para compor URLs sem duplicidade. */
    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

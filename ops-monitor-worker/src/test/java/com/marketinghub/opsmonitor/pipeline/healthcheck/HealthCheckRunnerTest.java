package com.marketinghub.opsmonitor.pipeline.healthcheck;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class HealthCheckRunnerTest {

    @Test
    void deveConsumirPendenciasERegistrarHeartbeat() {
        var backendClient = mock(HealthCheckBackendClient.class);
        var processor = mock(HealthCheckProcessor.class);
        var properties = new HealthCheckProperties(Duration.ofSeconds(2));
        var pending = new PendingModuleCheck("backend", "Backend principal", "BACKEND", "http://localhost:8080",
                "/actuator/health", "/actuator/logfile", "CRITICAL", 300);
        var output = new HealthCheckOutput("backend", Instant.parse("2026-06-23T12:00:00Z"), "ONLINE", 200, 10,
                "{\"status\":\"UP\"}", null);
        when(backendClient.fetchPendingChecks()).thenReturn(List.of(pending));
        when(processor.process(any(), any())).thenReturn(output);
        when(backendClient.sendHeartbeat(output)).thenReturn(Mono.empty());

        new HealthCheckRunner(backendClient, processor, properties).runOnce();

        verify(backendClient).fetchPendingChecks();
        verify(processor).process(any(), any(HealthCheckInput.class));
        verify(backendClient).sendHeartbeat(output);
    }
}

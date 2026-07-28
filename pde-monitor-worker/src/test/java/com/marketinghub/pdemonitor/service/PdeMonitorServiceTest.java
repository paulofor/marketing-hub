package com.marketinghub.pdemonitor.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pdemonitor.config.PdeMonitorProperties;
import com.marketinghub.pdemonitor.db.PdeMonitorRepository;
import com.marketinghub.pdemonitor.db.PdeMonitoredModule;
import com.marketinghub.pdemonitor.health.PdeHealthChecker;
import com.marketinghub.pdemonitor.health.PdeHealthResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida a coordenação do ciclo de monitoramento e incidentes dos PDEs. */
class PdeMonitorServiceTest {

    @Test
    /** Garante que uma falha abre incidente crítico quando não existe incidente aberto. */
    void deveAbrirIncidenteQuandoPdeCriticoFalha() {
        var repository = mock(PdeMonitorRepository.class);
        var checker = mock(PdeHealthChecker.class);
        var service = service(repository, checker);
        var module = module();
        var result = new PdeHealthResult(Instant.parse("2026-07-28T12:00:00Z"), "OFFLINE", null, 100, null, "erro");
        when(repository.findCriticalPdes()).thenReturn(List.of(module));
        when(checker.check(module)).thenReturn(result);
        when(repository.findOpenIncidentId(1)).thenReturn(Optional.empty());

        service.runOnce();

        verify(repository).insertHealthCheck(module, result);
        verify(repository).openIncident(module, result, "CRITICAL");
    }

    @Test
    /** Garante que um PDE recuperado encerra incidente previamente aberto. */
    void deveEncerrarIncidenteQuandoPdeVoltaOnline() {
        var repository = mock(PdeMonitorRepository.class);
        var checker = mock(PdeHealthChecker.class);
        var service = service(repository, checker);
        var module = module();
        var endedAt = Instant.parse("2026-07-28T12:05:00Z");
        var startedAt = Instant.parse("2026-07-28T12:00:00Z");
        var result = new PdeHealthResult(endedAt, "ONLINE", 200, 80, "ok", null);
        when(repository.findCriticalPdes()).thenReturn(List.of(module));
        when(checker.check(module)).thenReturn(result);
        when(repository.findOpenIncidentId(1)).thenReturn(Optional.of(55L));
        when(repository.findIncidentStartedAt(55L)).thenReturn(Optional.of(startedAt));

        service.runOnce();

        verify(repository).insertHealthCheck(module, result);
        verify(repository).closeIncident(55L, endedAt, startedAt);
        verify(repository, never()).openIncident(module, result, "CRITICAL");
    }

    /** Cria o serviço com configuração crítica padrão para testes. */
    private PdeMonitorService service(PdeMonitorRepository repository, PdeHealthChecker checker) {
        return new PdeMonitorService(
                repository,
                checker,
                new PdeMonitorProperties(
                        new PdeMonitorProperties.Http(Duration.ofSeconds(2), 3000),
                        new PdeMonitorProperties.Incident("CRITICAL")));
    }

    /** Cria um PDE monitorado de exemplo. */
    private PdeMonitoredModule module() {
        return new PdeMonitoredModule(
                1, "pde-musa-v6", "MUSA v6", "https://v6.clubemusa.com.br", "/healthz", null, 120);
    }
}

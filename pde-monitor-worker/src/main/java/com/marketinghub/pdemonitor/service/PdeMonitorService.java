package com.marketinghub.pdemonitor.service;

import com.marketinghub.pdemonitor.config.PdeMonitorProperties;
import com.marketinghub.pdemonitor.db.PdeMonitorRepository;
import com.marketinghub.pdemonitor.db.PdeMonitoredModule;
import com.marketinghub.pdemonitor.health.PdeHealthChecker;
import com.marketinghub.pdemonitor.health.PdeHealthResult;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Coordena o ciclo direto de monitoramento dos PDEs críticos. */
@Service
public class PdeMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PdeMonitorService.class);
    private final PdeMonitorRepository repository;
    private final PdeHealthChecker checker;
    private final PdeMonitorProperties properties;

    /** Recebe as dependências de banco, verificação HTTP e configuração operacional. */
    public PdeMonitorService(
            PdeMonitorRepository repository, PdeHealthChecker checker, PdeMonitorProperties properties) {
        this.repository = repository;
        this.checker = checker;
        this.properties = properties;
    }

    /** Executa a verificação periódica dos PDEs críticos a cada minuto. */
    @Scheduled(cron = "0 */1 * * * *")
    public void runScheduledChecks() {
        runOnce();
    }

    /** Executa um ciclo completo de monitoramento para testes ou acionamento manual. */
    public void runOnce() {
        var modules = repository.findCriticalPdes();
        log.info("pde-monitor-worker ciclo iniciado: pdes={}", modules.size());
        modules.forEach(this::processModule);
    }

    /** Verifica um PDE, grava heartbeat e sincroniza incidente aberto. */
    void processModule(PdeMonitoredModule module) {
        try {
            PdeHealthResult result = checker.check(module);
            repository.insertHealthCheck(module, result);
            synchronizeIncident(module, result);
            log.info(
                    "pde-monitor-worker verificacao registrada: module={} status={} httpStatus={} responseTimeMs={}",
                    module.code(),
                    result.status(),
                    result.httpStatus(),
                    result.responseTimeMs());
        } catch (RuntimeException ex) {
            log.error("pde-monitor-worker falhou ao monitorar PDE: module={}", module.code(), ex);
        }
    }

    /** Abre incidente quando há falha e encerra quando o PDE volta ao normal. */
    private void synchronizeIncident(PdeMonitoredModule module, PdeHealthResult result) {
        var openIncidentId = repository.findOpenIncidentId(module.id());
        if (result.online()) {
            openIncidentId.ifPresent(id -> closeIncident(id, result.checkedAt()));
            return;
        }
        if (openIncidentId.isEmpty()) {
            repository.openIncident(module, result, properties.incidentSeverity());
        }
    }

    /** Encerra um incidente preservando a duração calculada a partir do banco. */
    private void closeIncident(long incidentId, Instant endedAt) {
        repository
                .findIncidentStartedAt(incidentId)
                .ifPresent(startedAt -> repository.closeIncident(incidentId, endedAt, startedAt));
    }
}

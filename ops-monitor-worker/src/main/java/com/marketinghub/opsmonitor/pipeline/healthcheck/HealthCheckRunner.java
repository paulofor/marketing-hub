package com.marketinghub.opsmonitor.pipeline.healthcheck;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Executa periodicamente as verificações de saúde entregues pelo backend. */
public class HealthCheckRunner {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckRunner.class);
    private final HealthCheckBackendClient backendClient;
    private final HealthCheckProcessor processor;
    private final HealthCheckProperties properties;

    /** Recebe as dependências usadas para buscar pendências, verificar saúde e reportar heartbeat. */
    public HealthCheckRunner(HealthCheckBackendClient backendClient, HealthCheckProcessor processor,
            HealthCheckProperties properties) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.properties = properties;
    }

    /** Executa um ciclo periódico completo de verificação dos módulos pendentes. */
    @Scheduled(cron = "0 */1 * * * *")
    public void runScheduledChecks() {
        runOnce();
    }

    /** Executa um ciclo imediato de verificação para reuso em testes e inicializações manuais. */
    public void runOnce() {
        var pendingChecks = backendClient.fetchPendingChecks();
        log.info("ops-monitor-worker healthcheck ciclo iniciado: modules={}", pendingChecks.size());
        pendingChecks.forEach(this::processModule);
    }

    /** Processa um único módulo e registra o heartbeat mesmo quando a chamada de saúde falha. */
    private void processModule(PendingModuleCheck check) {
        String targetUrl = targetUrl(check);
        try {
            var output = processor.process(StageContext.simple("healthcheck-" + check.moduleCode(), check.moduleCode()),
                    new HealthCheckInput(check.moduleCode(), targetUrl, defaultTimeout()));
            backendClient.sendHeartbeat(output).block(defaultTimeout());
            log.info("ops-monitor-worker heartbeat registrado: module={} status={} url={}", check.moduleCode(), output.status(), targetUrl);
        } catch (RuntimeException ex) {
            log.error("ops-monitor-worker falhou ao processar heartbeat: module={} url={}", check.moduleCode(), targetUrl, ex);
        }
    }

    /** Monta a URL final de saúde a partir do contrato recebido do backend. */
    private String targetUrl(PendingModuleCheck check) {
        String baseUrl = stripTrailingSlash(check.baseUrl());
        String healthPath = check.healthPath() == null || check.healthPath().isBlank() ? "/actuator/health" : check.healthPath();
        return baseUrl + (healthPath.startsWith("/") ? healthPath : "/" + healthPath);
    }

    /** Remove barras finais para evitar URL com separador duplicado. */
    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Resolve o timeout padrão configurado para as chamadas de saúde e reporte. */
    private Duration defaultTimeout() {
        return properties.defaultTimeout() == null ? Duration.ofSeconds(5) : properties.defaultTimeout();
    }
}

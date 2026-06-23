package com.marketinghub.opsmonitor.controller;

import com.marketinghub.opsmonitor.service.OpsMonitorService;
import com.marketinghub.opsmonitor.service.listAvailability.ModuleAvailabilityResponse;
import com.marketinghub.opsmonitor.service.listAvailabilityHistory.ModuleAvailabilityHistoryResponse;
import com.marketinghub.opsmonitor.service.listIncidents.ModuleIncidentResponse;
import com.marketinghub.opsmonitor.service.listPendingChecks.PendingModuleCheckResponse;
import com.marketinghub.opsmonitor.service.registerHeartbeat.RegisterModuleHeartbeatRequest;
import com.marketinghub.opsmonitor.service.registerHeartbeat.RegisterModuleHeartbeatResponse;
import com.marketinghub.opsmonitor.service.registerIncident.RegisterModuleIncidentRequest;
import com.marketinghub.opsmonitor.service.registerIncident.RegisterModuleIncidentResponse;
import com.marketinghub.opsmonitor.service.summary.OpsMonitorSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe contratos internos e administrativos do monitoramento operacional. */
@RestController
@RequestMapping
public class OpsMonitorController {
    private final OpsMonitorService service;

    public OpsMonitorController(OpsMonitorService service) {
        this.service = service;
    }

    /** Entrega ao worker a lista de módulos pendentes para verificação operacional. */
    @GetMapping("/api/internal/ops-monitor/v1/module-checks/stage-executions/pending")
    public List<PendingModuleCheckResponse> pending() {
        return service.listPendingChecks();
    }

    /** Recebe o heartbeat de saúde de um módulo monitorado. */
    @PostMapping("/api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat")
    public RegisterModuleHeartbeatResponse registerHeartbeat(@PathVariable String moduleCode,
            @RequestBody RegisterModuleHeartbeatRequest request) {
        return service.registerHeartbeat(moduleCode, request);
    }

    /** Recebe um incidente operacional identificado pelo worker. */
    @PostMapping("/api/internal/ops-monitor/v1/modules/{moduleCode}/incidents")
    public RegisterModuleIncidentResponse registerIncident(@PathVariable String moduleCode,
            @RequestBody RegisterModuleIncidentRequest request) {
        return service.registerIncident(moduleCode, request);
    }

    /** Retorna o resumo executivo para a tela administrativa. */
    @GetMapping("/api/ops-monitor/v1/summary")
    public OpsMonitorSummaryResponse summary() {
        return service.getSummary();
    }

    /** Retorna o status atual dos módulos monitorados. */
    @GetMapping("/api/ops-monitor/v1/modules/availability")
    public List<ModuleAvailabilityResponse> listAvailability() {
        return service.listAvailability();
    }

    /** Retorna o histórico diário de disponibilidade de um módulo. */
    @GetMapping("/api/ops-monitor/v1/modules/{moduleCode}/availability-history")
    public List<ModuleAvailabilityHistoryResponse> listAvailabilityHistory(@PathVariable String moduleCode) {
        return service.listAvailabilityHistory(moduleCode);
    }

    /** Retorna incidentes operacionais abertos. */
    @GetMapping("/api/ops-monitor/v1/incidents/open")
    public List<ModuleIncidentResponse> listOpenIncidents() {
        return service.listIncidents(true);
    }

    /** Retorna o histórico recente de incidentes operacionais. */
    @GetMapping("/api/ops-monitor/v1/incidents/history")
    public List<ModuleIncidentResponse> listIncidentHistory() {
        return service.listIncidents(false);
    }
}

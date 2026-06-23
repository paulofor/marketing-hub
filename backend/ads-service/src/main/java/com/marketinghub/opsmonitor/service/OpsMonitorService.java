package com.marketinghub.opsmonitor.service;

import com.marketinghub.opsmonitor.OpsModuleHealthCheck;
import com.marketinghub.opsmonitor.OpsModuleIncident;
import com.marketinghub.opsmonitor.OpsMonitoredModule;
import com.marketinghub.opsmonitor.service.listAvailability.ModuleAvailabilityResponse;
import com.marketinghub.opsmonitor.service.listAvailabilityHistory.ModuleAvailabilityHistoryResponse;
import com.marketinghub.opsmonitor.service.listIncidents.ModuleIncidentResponse;
import com.marketinghub.opsmonitor.service.listPendingChecks.PendingModuleCheckResponse;
import com.marketinghub.opsmonitor.service.registerHeartbeat.RegisterModuleHeartbeatRequest;
import com.marketinghub.opsmonitor.service.registerHeartbeat.RegisterModuleHeartbeatResponse;
import com.marketinghub.opsmonitor.service.registerIncident.RegisterModuleIncidentRequest;
import com.marketinghub.opsmonitor.service.registerIncident.RegisterModuleIncidentResponse;
import com.marketinghub.opsmonitor.service.summary.OpsMonitorSummaryResponse;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleAvailabilityDailyRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleHealthCheckRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleIncidentRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsMonitoredModuleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra contratos, persistência e consultas administrativas do monitoramento operacional. */
@Service
public class OpsMonitorService {
    private final OpsMonitoredModuleRepository moduleRepository;
    private final OpsModuleHealthCheckRepository healthCheckRepository;
    private final OpsModuleIncidentRepository incidentRepository;
    private final OpsModuleAvailabilityDailyRepository availabilityDailyRepository;

    public OpsMonitorService(OpsMonitoredModuleRepository moduleRepository,
            OpsModuleHealthCheckRepository healthCheckRepository,
            OpsModuleIncidentRepository incidentRepository,
            OpsModuleAvailabilityDailyRepository availabilityDailyRepository) {
        this.moduleRepository = moduleRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.incidentRepository = incidentRepository;
        this.availabilityDailyRepository = availabilityDailyRepository;
    }

    /** Lista módulos habilitados para o worker executar verificações pendentes. */
    @Transactional(readOnly = true)
    public List<PendingModuleCheckResponse> listPendingChecks() {
        return moduleRepository.findByEnabledTrueOrderByCodeAsc().stream()
                .map(module -> new PendingModuleCheckResponse(module.getCode(), module.getName(), module.getType(),
                        module.getBaseUrl(), module.getHealthPath(), module.getLogPath(), module.getCriticality(),
                        module.getOfflineThresholdSeconds()))
                .toList();
    }

    /** Registra o heartbeat recebido do worker para um módulo monitorado. */
    @Transactional
    public RegisterModuleHeartbeatResponse registerHeartbeat(String moduleCode, RegisterModuleHeartbeatRequest request) {
        OpsMonitoredModule module = findModule(moduleCode);
        OpsModuleHealthCheck check = new OpsModuleHealthCheck();
        check.setModule(module);
        check.setCheckedAt(request.checkedAt() == null ? Instant.now() : request.checkedAt());
        check.setStatus(request.status());
        check.setHttpStatus(request.httpStatus());
        check.setResponseTimeMs(request.responseTimeMs());
        check.setErrorMessage(request.errorMessage());
        check.setRawPayload(request.rawPayload());
        OpsModuleHealthCheck saved = healthCheckRepository.save(check);
        return new RegisterModuleHeartbeatResponse(saved.getId(), module.getCode(), saved.getStatus());
    }

    /** Registra um incidente operacional informado pelo worker. */
    @Transactional
    public RegisterModuleIncidentResponse registerIncident(String moduleCode, RegisterModuleIncidentRequest request) {
        OpsMonitoredModule module = findModule(moduleCode);
        OpsModuleIncident incident = new OpsModuleIncident();
        incident.setModule(module);
        incident.setStatus(request.status());
        incident.setSeverity(request.severity());
        incident.setStartedAt(request.startedAt() == null ? Instant.now() : request.startedAt());
        incident.setEndedAt(request.endedAt());
        incident.setDurationSeconds(request.durationSeconds());
        incident.setSummary(request.summary());
        incident.setRootSignal(request.rootSignal());
        incident.setLastError(request.lastError());
        OpsModuleIncident saved = incidentRepository.save(incident);
        return new RegisterModuleIncidentResponse(saved.getId(), module.getCode(), saved.getStatus(), saved.getSeverity());
    }

    /** Lista a disponibilidade atual calculada a partir do último heartbeat de cada módulo. */
    @Transactional(readOnly = true)
    public List<ModuleAvailabilityResponse> listAvailability() {
        return moduleRepository.findAll().stream().map(this::toAvailabilityResponse).toList();
    }

    /** Lista o histórico diário de disponibilidade do módulo informado. */
    @Transactional(readOnly = true)
    public List<ModuleAvailabilityHistoryResponse> listAvailabilityHistory(String moduleCode) {
        return availabilityDailyRepository.findTop30ByModuleCodeOrderByAvailabilityDateDesc(moduleCode).stream()
                .map(day -> new ModuleAvailabilityHistoryResponse(day.getAvailabilityDate(), day.getTotalChecks(),
                        day.getSuccessfulChecks(), day.getFailedChecks(), day.getAvailabilityPercentage(),
                        day.getOfflineSeconds(), day.getDegradedSeconds()))
                .toList();
    }

    /** Lista incidentes abertos ou o histórico recente de incidentes. */
    @Transactional(readOnly = true)
    public List<ModuleIncidentResponse> listIncidents(boolean openOnly) {
        List<OpsModuleIncident> incidents = openOnly
                ? incidentRepository.findByStatusOrderByStartedAtDesc("OPEN")
                : incidentRepository.findTop100ByOrderByStartedAtDesc();
        return incidents.stream().map(this::toIncidentResponse).toList();
    }

    /** Gera resumo executivo para a tela administrativa de operação. */
    @Transactional(readOnly = true)
    public OpsMonitorSummaryResponse getSummary() {
        List<ModuleAvailabilityResponse> availability = listAvailability();
        long online = availability.stream().filter(item -> "ONLINE".equals(item.status())).count();
        long degraded = availability.stream().filter(item -> "DEGRADED".equals(item.status())).count();
        long offline = availability.stream().filter(item -> "OFFLINE".equals(item.status())).count();
        long unknown = availability.stream().filter(item -> "UNKNOWN".equals(item.status())).count();
        long openIncidents = incidentRepository.findByStatusOrderByStartedAtDesc("OPEN").size();
        return new OpsMonitorSummaryResponse(online, degraded, offline, unknown, openIncidents);
    }

    /** Busca um módulo monitorado pelo código e falha quando ele não existe. */
    private OpsMonitoredModule findModule(String moduleCode) {
        return moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("Módulo monitorado não encontrado: " + moduleCode));
    }

    /** Converte entidade de módulo para o status administrativo atual. */
    private ModuleAvailabilityResponse toAvailabilityResponse(OpsMonitoredModule module) {
        return healthCheckRepository.findTop1ByModuleCodeOrderByCheckedAtDesc(module.getCode())
                .map(check -> new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), check.getStatus(), check.getCheckedAt(), check.getResponseTimeMs(),
                        check.getErrorMessage()))
                .orElseGet(() -> new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), "UNKNOWN", null, null, null));
    }

    /** Converte entidade de incidente para resposta administrativa. */
    private ModuleIncidentResponse toIncidentResponse(OpsModuleIncident incident) {
        OpsMonitoredModule module = incident.getModule();
        return new ModuleIncidentResponse(incident.getId(), module.getCode(), module.getName(), incident.getStatus(),
                incident.getSeverity(), incident.getStartedAt(), incident.getEndedAt(), incident.getDurationSeconds(),
                incident.getSummary(), incident.getRootSignal(), incident.getLastError());
    }
}

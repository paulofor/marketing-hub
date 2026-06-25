package com.marketinghub.opsmonitor.service;

import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
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
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleAvailabilityDailyRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleHealthCheckRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleIncidentRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsMonitoredModuleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra contratos, persistência e consultas administrativas do monitoramento operacional. */
@Service
public class OpsMonitorService {
    private final OpsMonitoredModuleRepository moduleRepository;
    private final OpsModuleHealthCheckRepository healthCheckRepository;
    private final OpsModuleIncidentRepository incidentRepository;
    private static final String AI_WORKER_MODULE_CODE = "ai-worker";
    private static final String OPRM_COLETOR_MEI_MODULE_CODE = "oprm-coletor-mei";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_OPEN = "OPEN";
    private static final Duration AI_WORKER_STALE_QUEUE_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration OPRM_NICHO_CNAE_V3_STALE_QUEUE_THRESHOLD = Duration.ofMinutes(6);
    private static final List<String> AI_WORKER_GERALANDING_STAGES = List.of(
            "landing-page-wireframe",
            "landing-page-copy",
            "landing-page-image-planning",
            "landing-page-image-generation",
            "landing-page-design-preset",
            "landing-page-quality-review",
            "landing-page-deliverables");

    private final OpsModuleAvailabilityDailyRepository availabilityDailyRepository;
    private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
    private final OprmNichoCnaeV3StageExecutionRepository nichoCnaeV3StageExecutionRepository;

    /** Inicializa o serviço com repositórios de monitoramento e filas auditáveis dos pipelines. */
    public OpsMonitorService(OpsMonitoredModuleRepository moduleRepository,
            OpsModuleHealthCheckRepository healthCheckRepository,
            OpsModuleIncidentRepository incidentRepository,
            OpsModuleAvailabilityDailyRepository availabilityDailyRepository,
            GeraLandingStageExecutionRepository geraLandingStageExecutionRepository,
            OprmNichoCnaeV3StageExecutionRepository nichoCnaeV3StageExecutionRepository) {
        this.moduleRepository = moduleRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.incidentRepository = incidentRepository;
        this.availabilityDailyRepository = availabilityDailyRepository;
        this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
        this.nichoCnaeV3StageExecutionRepository = nichoCnaeV3StageExecutionRepository;
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
        return listAvailability(null, null);
    }

    /** Lista a disponibilidade atual filtrada por criticidade e tipo quando informado. */
    @Transactional(readOnly = true)
    public List<ModuleAvailabilityResponse> listAvailability(String criticality, String type) {
        return moduleRepository.findAll().stream()
                .filter(module -> matchesFilter(module.getCriticality(), criticality))
                .filter(module -> matchesFilter(module.getType(), type))
                .map(this::toAvailabilityResponse)
                .toList();
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
        return listIncidents(openOnly, null, null);
    }

    /** Lista incidentes operacionais filtrados por criticidade e tipo quando informado. */
    @Transactional(readOnly = true)
    public List<ModuleIncidentResponse> listIncidents(boolean openOnly, String criticality, String type) {
        List<OpsModuleIncident> incidents = openOnly
                ? incidentRepository.findByStatusOrderByStartedAtDesc(STATUS_OPEN)
                : incidentRepository.findTop100ByOrderByStartedAtDesc();
        List<ModuleIncidentResponse> responses = new ArrayList<>(incidents.stream()
                .filter(incident -> matchesFilter(incident.getModule().getCriticality(), criticality))
                .filter(incident -> matchesFilter(incident.getModule().getType(), type))
                .map(this::toIncidentResponse)
                .toList());
        syntheticAiWorkerQueueIncident(openOnly)
                .filter(incident -> matchesFilter(incident.criticality(), criticality))
                .filter(incident -> matchesFilter(incident.type(), type))
                .map(this::toIncidentResponse)
                .ifPresent(responses::add);
        syntheticOprmNichoCnaeV3QueueIncident(openOnly)
                .filter(incident -> matchesFilter(incident.criticality(), criticality))
                .filter(incident -> matchesFilter(incident.type(), type))
                .map(this::toIncidentResponse)
                .ifPresent(responses::add);
        return responses;
    }

    /** Gera resumo executivo para a tela administrativa de operação. */
    @Transactional(readOnly = true)
    public OpsMonitorSummaryResponse getSummary() {
        List<ModuleAvailabilityResponse> availability = listAvailability();
        long online = availability.stream().filter(item -> "ONLINE".equals(item.status())).count();
        long degraded = availability.stream().filter(item -> "DEGRADED".equals(item.status())).count();
        long offline = availability.stream().filter(item -> "OFFLINE".equals(item.status())).count();
        long unknown = availability.stream().filter(item -> "UNKNOWN".equals(item.status())).count();
        long openIncidents = incidentRepository.findByStatusOrderByStartedAtDesc(STATUS_OPEN).size()
                + syntheticAiWorkerQueueIncident(true).map(incident -> 1L).orElse(0L)
                + syntheticOprmNichoCnaeV3QueueIncident(true).map(incident -> 1L).orElse(0L);
        return new OpsMonitorSummaryResponse(online, degraded, offline, unknown, openIncidents);
    }

    /** Valida se um valor passa pelo filtro opcional recebido da tela. */
    private boolean matchesFilter(String value, String filter) {
        return filter == null || filter.isBlank() || value.equalsIgnoreCase(filter);
    }

    /** Busca um módulo monitorado pelo código e falha quando ele não existe. */
    private OpsMonitoredModule findModule(String moduleCode) {
        return moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("Módulo monitorado não encontrado: " + moduleCode));
    }

    /** Converte entidade de módulo para o status administrativo atual. */
    private ModuleAvailabilityResponse toAvailabilityResponse(OpsMonitoredModule module) {
        if (AI_WORKER_MODULE_CODE.equals(module.getCode())) {
            Optional<SyntheticIncident> queueIncident = syntheticAiWorkerQueueIncident(true);
            if (queueIncident.isPresent()) {
                SyntheticIncident incident = queueIncident.get();
                return new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), "DEGRADED", incident.startedAt(), null, incident.lastError());
            }
        }
        if (OPRM_COLETOR_MEI_MODULE_CODE.equals(module.getCode())) {
            Optional<SyntheticIncident> queueIncident = syntheticOprmNichoCnaeV3QueueIncident(true);
            if (queueIncident.isPresent()) {
                SyntheticIncident incident = queueIncident.get();
                return new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), "DEGRADED", incident.startedAt(), null, incident.lastError());
            }
        }
        return healthCheckRepository.findTop1ByModuleCodeOrderByCheckedAtDesc(module.getCode())
                .map(check -> new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), check.getStatus(), check.getCheckedAt(), check.getResponseTimeMs(),
                        check.getErrorMessage()))
                .orElseGet(() -> new ModuleAvailabilityResponse(module.getCode(), module.getName(), module.getType(),
                        module.getCriticality(), "UNKNOWN", null, null, null));
    }

    /** Cria incidente sintético quando há fila de GeraLanding parada antes do worker iniciar processamento. */
    private Optional<SyntheticIncident> syntheticAiWorkerQueueIncident(boolean openOnly) {
        if (!openOnly) {
            return Optional.empty();
        }
        Instant threshold = Instant.now().minus(AI_WORKER_STALE_QUEUE_THRESHOLD);
        for (String stageCode : AI_WORKER_GERALANDING_STAGES) {
            List<GeraLandingStageExecution> staleExecutions = geraLandingStageExecutionRepository
                    .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                            stageCode,
                            STATUS_STARTED,
                            threshold);
            if (!staleExecutions.isEmpty()) {
                GeraLandingStageExecution execution = staleExecutions.getFirst();
                String jobId = new String(execution.getIdJob(), StandardCharsets.UTF_8);
                String lastError = "Job " + jobId + " do experimento " + execution.getExperimentId()
                        + " está em INICIADO sem processamento iniciado na etapa " + stageCode + ".";
                return Optional.of(new SyntheticIncident(
                        AI_WORKER_MODULE_CODE,
                        "AI Worker",
                        "WORKER",
                        "CRITICAL",
                        STATUS_OPEN,
                        "HIGH",
                        execution.getExecutionRequestedAt(),
                        "AI Worker não consumiu job pendente do GeraLanding",
                        "GERALANDING_QUEUE_STALE",
                        lastError));
            }
        }
        return Optional.empty();
    }

    /** Cria incidente sintético quando há pendência antiga no pipeline OPRM NichoCNAE v3. */
    private Optional<SyntheticIncident> syntheticOprmNichoCnaeV3QueueIncident(boolean openOnly) {
        if (!openOnly) {
            return Optional.empty();
        }
        Instant threshold = Instant.now().minus(OPRM_NICHO_CNAE_V3_STALE_QUEUE_THRESHOLD);
        List<OprmNichoCnaeV3StageExecution> staleExecutions = nichoCnaeV3StageExecutionRepository
                .findTop20ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        OprmNichoCnaeV3StageExecutionStatus.PENDING,
                        threshold);
        if (staleExecutions.isEmpty()) {
            return Optional.empty();
        }
        OprmNichoCnaeV3StageExecution execution = staleExecutions.getFirst();
        String lastError = "Job " + execution.getJobId() + " do CNAE " + execution.getCnaeCode()
                + " está PENDING na etapa " + execution.getStageCode()
                + " sem consumo pelo executor NichoCNAE v3.";
        return Optional.of(new SyntheticIncident(
                OPRM_COLETOR_MEI_MODULE_CODE,
                "OPRM Coletor MEI",
                "COLLECTOR",
                "HIGH",
                STATUS_OPEN,
                "HIGH",
                execution.getCreatedAt(),
                "OPRM Coletor MEI não consumiu pendência do NichoCNAE v3",
                "OPRM_NICHO_CNAE_V3_QUEUE_STALE",
                lastError));
    }

    /** Converte incidente sintético para resposta administrativa sem persistir novo estado operacional. */
    private ModuleIncidentResponse toIncidentResponse(SyntheticIncident incident) {
        return new ModuleIncidentResponse(null, incident.moduleCode(), incident.moduleName(), incident.status(),
                incident.severity(), incident.startedAt(), null, null, incident.summary(), incident.rootSignal(),
                incident.lastError());
    }

    /** Converte entidade de incidente para resposta administrativa. */
    private ModuleIncidentResponse toIncidentResponse(OpsModuleIncident incident) {
        OpsMonitoredModule module = incident.getModule();
        return new ModuleIncidentResponse(incident.getId(), module.getCode(), module.getName(), incident.getStatus(),
                incident.getSeverity(), incident.getStartedAt(), incident.getEndedAt(), incident.getDurationSeconds(),
                incident.getSummary(), incident.getRootSignal(), incident.getLastError());
    }
}

/** Incidente calculado a partir de filas persistidas para destacar falha visível ao usuário. */
record SyntheticIncident(String moduleCode, String moduleName, String type, String criticality, String status,
        String severity, Instant startedAt, String summary, String rootSignal, String lastError) {}

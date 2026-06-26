package com.marketinghub.opsmonitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.opsmonitor.OpsModuleHealthCheck;
import com.marketinghub.opsmonitor.OpsMonitoredModule;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleAvailabilityDailyRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleHealthCheckRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleIncidentRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsMonitoredModuleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import jakarta.persistence.Column;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida os contratos básicos do service canônico do Ops Monitor. */
@ExtendWith(MockitoExtension.class)
class OpsMonitorServiceTest {
    @Mock
    private OpsMonitoredModuleRepository moduleRepository;

    @Mock
    private OpsModuleHealthCheckRepository healthCheckRepository;

    @Mock
    private OpsModuleIncidentRepository incidentRepository;

    @Mock
    private OpsModuleAvailabilityDailyRepository availabilityDailyRepository;

    @Mock
    private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;

    @Mock
    private OprmNichoCnaeV3StageExecutionRepository nichoCnaeV3StageExecutionRepository;

    /** Garante que o pending retorna somente módulos habilitados pelo cadastro operacional. */
    @Test
    void listPendingChecksReturnsEnabledModules() {
        OpsMonitoredModule module = new OpsMonitoredModule();
        module.setCode("backend");
        module.setName("Backend principal");
        module.setType("BACKEND");
        module.setBaseUrl("http://191.252.181.168");
        module.setHealthPath("/actuator/health");
        module.setLogPath("/actuator/logfile");
        module.setCriticality("CRITICAL");
        module.setOfflineThresholdSeconds(300);
        when(moduleRepository.findByEnabledTrueOrderByCodeAsc()).thenReturn(List.of(module));

        OpsMonitorService service = new OpsMonitorService(
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository,
                geraLandingStageExecutionRepository, nichoCnaeV3StageExecutionRepository);

        var pending = service.listPendingChecks();

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().moduleCode()).isEqualTo("backend");
        assertThat(pending.getFirst().healthPath()).isEqualTo("/actuator/health");
    }

    /** Garante que a disponibilidade respeita filtros administrativos por criticidade e tipo. */
    @Test
    void listAvailabilityFiltersByCriticalityAndType() {
        OpsMonitoredModule criticalWorker = new OpsMonitoredModule();
        criticalWorker.setCode("ai-worker");
        criticalWorker.setName("AI Worker");
        criticalWorker.setType("WORKER");
        criticalWorker.setBaseUrl("http://191.252.120.96:4567/");
        criticalWorker.setHealthPath("worker-observability/health");
        criticalWorker.setCriticality("CRITICAL");

        OpsMonitoredModule highCollector = new OpsMonitoredModule();
        highCollector.setCode("mois-clickbank-collector");
        highCollector.setName("MOIS ClickBank Collector");
        highCollector.setType("COLLECTOR");
        highCollector.setCriticality("HIGH");

        when(moduleRepository.findAll()).thenReturn(List.of(criticalWorker, highCollector));
        when(healthCheckRepository.findTop1ByModuleCodeOrderByCheckedAtDesc("ai-worker")).thenReturn(Optional.empty());

        OpsMonitorService service = new OpsMonitorService(
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository,
                geraLandingStageExecutionRepository, nichoCnaeV3StageExecutionRepository);

        var filtered = service.listAvailability("CRITICAL", "WORKER");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.getFirst().moduleCode()).isEqualTo("ai-worker");
        assertThat(filtered.getFirst().attemptedUrl()).isEqualTo("http://191.252.120.96:4567/worker-observability/health");
    }

    /** Garante que fila antiga de GeraLanding aparece como incidente do AI Worker. */
    @Test
    void listIncidentsAddsSyntheticAiWorkerQueueIncident() {
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(48L)
                .stageCode("landing-page-wireframe")
                .status("INICIADO")
                .executionRequestedAt(Instant.now().minusSeconds(600))
                .idJob("job-stale".getBytes(StandardCharsets.UTF_8))
                .build();
        when(incidentRepository.findByStatusOrderByStartedAtDesc("OPEN")).thenReturn(List.of());
        when(geraLandingStageExecutionRepository
                .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                        org.mockito.ArgumentMatchers.eq("landing-page-wireframe"),
                        org.mockito.ArgumentMatchers.eq("INICIADO"),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(execution));

        OpsMonitorService service = new OpsMonitorService(
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository,
                geraLandingStageExecutionRepository, nichoCnaeV3StageExecutionRepository);

        var incidents = service.listIncidents(true, "CRITICAL", "WORKER");

        assertThat(incidents).hasSize(1);
        assertThat(incidents.getFirst().moduleCode()).isEqualTo("ai-worker");
        assertThat(incidents.getFirst().rootSignal()).isEqualTo("GERALANDING_QUEUE_STALE");
        assertThat(incidents.getFirst().lastError()).contains("job-stale");
    }

    /** Garante que fila antiga do NichoCNAE v3 aparece como incidente do OPRM Coletor MEI. */
    @Test
    void listIncidentsAddsSyntheticOprmNichoCnaeV3QueueIncident() {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setJobId("nichocnae-v3-7319002-1782413765640");
        execution.setCnaeCode("7319002");
        execution.setStageCode("cnae-intake");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setCreatedAt(Instant.now().minusSeconds(900));
        when(incidentRepository.findByStatusOrderByStartedAtDesc("OPEN")).thenReturn(List.of());
        when(geraLandingStageExecutionRepository
                .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq("INICIADO"),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of());
        when(nichoCnaeV3StageExecutionRepository.findTop20ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        org.mockito.ArgumentMatchers.eq(OprmNichoCnaeV3StageExecutionStatus.PENDING),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(execution));

        OpsMonitorService service = new OpsMonitorService(
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository,
                geraLandingStageExecutionRepository, nichoCnaeV3StageExecutionRepository);

        var incidents = service.listIncidents(true, "HIGH", "COLLECTOR");

        assertThat(incidents).hasSize(1);
        assertThat(incidents.getFirst().moduleCode()).isEqualTo("oprm-coletor-mei");
        assertThat(incidents.getFirst().rootSignal()).isEqualTo("OPRM_NICHO_CNAE_V3_QUEUE_STALE");
        assertThat(incidents.getFirst().lastError()).contains("7319002").contains("cnae-intake");
    }

    /** Garante que fila v3 parada degrada a disponibilidade do OPRM Coletor MEI. */
    @Test
    void listAvailabilityMarksOprmCollectorDegradedWhenV3QueueIsStale() {
        OpsMonitoredModule collector = new OpsMonitoredModule();
        collector.setCode("oprm-coletor-mei");
        collector.setName("OPRM Coletor MEI");
        collector.setType("COLLECTOR");
        collector.setBaseUrl("http://191.252.181.168");
        collector.setHealthPath("/actuator/health");
        collector.setCriticality("HIGH");

        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setJobId("nichocnae-v3-7319002-1782413765640");
        execution.setCnaeCode("7319002");
        execution.setStageCode("cnae-intake");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setCreatedAt(Instant.now().minusSeconds(900));
        when(moduleRepository.findAll()).thenReturn(List.of(collector));
        when(nichoCnaeV3StageExecutionRepository.findTop20ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        org.mockito.ArgumentMatchers.eq(OprmNichoCnaeV3StageExecutionStatus.PENDING),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(execution));

        OpsMonitorService service = new OpsMonitorService(
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository,
                geraLandingStageExecutionRepository, nichoCnaeV3StageExecutionRepository);

        var availability = service.listAvailability("HIGH", "COLLECTOR");

        assertThat(availability).hasSize(1);
        assertThat(availability.getFirst().moduleCode()).isEqualTo("oprm-coletor-mei");
        assertThat(availability.getFirst().status()).isEqualTo("DEGRADED");
        assertThat(availability.getFirst().lastError()).contains("NichoCNAE v3");
        assertThat(availability.getFirst().attemptedUrl()).isEqualTo("http://191.252.181.168/actuator/health");
    }

    /** Garante que o payload bruto de healthcheck suporta respostas Actuator maiores que 255 caracteres. */
    @Test
    void rawPayloadColumnUsesLongText() throws Exception {
        Column column = OpsModuleHealthCheck.class.getDeclaredField("rawPayload").getAnnotation(Column.class);

        assertThat(column.columnDefinition()).isEqualTo("LONGTEXT");
    }

}

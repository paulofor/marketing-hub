package com.marketinghub.opsmonitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.opsmonitor.OpsMonitoredModule;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleAvailabilityDailyRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleHealthCheckRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsModuleIncidentRepository;
import com.marketinghub.repository.jpa.opsmonitor.OpsMonitoredModuleRepository;
import java.util.List;
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
                moduleRepository, healthCheckRepository, incidentRepository, availabilityDailyRepository);

        var pending = service.listPendingChecks();

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().moduleCode()).isEqualTo("backend");
        assertThat(pending.getFirst().healthPath()).isEqualTo("/actuator/health");
    }
}

package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisAutomationDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoisHotmartRobotServiceTest {

    @Mock
    private MoisDomainService domainService;

    private MoisHotmartRobotService service;

    @BeforeEach
    void setup() {
        MoisHotmartRobotProperties properties = new MoisHotmartRobotProperties();
        properties.setWorkspaceId("workspace-001");
        properties.setNiche("marketing-digital");
        properties.setMarketTheme("ofertas-quentes");
        properties.setSources(List.of("HOTMART"));
        properties.setTimeWindow("LAST_7_DAYS");
        properties.setLimitPerSource(25);
        properties.setLocale("pt-BR");
        properties.setCountry("BR");
        properties.setMinSuccessScore(80);
        service = new MoisHotmartRobotService(domainService, properties);
    }

    @Test
    void shouldRunManualJobAndStoreHistory() {
        when(domainService.createCollectionJob(any(MoisWorkspaceDtos.CreateCollectionJobRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.CollectionJobResponse(
                        "job-001",
                        "workspace-001",
                        "marketing-digital",
                        "ofertas-quentes",
                        "COMPLETED",
                        "LAST_7_DAYS",
                        25,
                        80,
                        List.of("HOTMART"),
                        null
                ));

        MoisAutomationDtos.HotmartRobotRunResponse run = service.triggerManualRun();

        assertThat(run.status()).isEqualTo("SUCCESS");
        assertThat(run.collectionJobId()).isEqualTo("job-001");
        assertThat(service.listRuns(10).items()).hasSize(1);
        verify(domainService, times(1)).createCollectionJob(any(MoisWorkspaceDtos.CreateCollectionJobRequest.class));
    }

    @Test
    void shouldFallbackToHotmartSourceWhenConfigIsEmpty() {
        MoisHotmartRobotProperties properties = new MoisHotmartRobotProperties();
        properties.setWorkspaceId("workspace-001");
        properties.setNiche("marketing-digital");
        properties.setMarketTheme("ofertas-quentes");
        properties.setSources(List.of());
        properties.setTimeWindow("LAST_7_DAYS");
        properties.setLimitPerSource(25);
        properties.setLocale("pt-BR");
        properties.setCountry("BR");
        properties.setMinSuccessScore(80);
        MoisHotmartRobotService localService = new MoisHotmartRobotService(domainService, properties);

        when(domainService.createCollectionJob(any(MoisWorkspaceDtos.CreateCollectionJobRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.CollectionJobResponse(
                        "job-001",
                        "workspace-001",
                        "marketing-digital",
                        "ofertas-quentes",
                        "COMPLETED",
                        "LAST_7_DAYS",
                        25,
                        80,
                        List.of("HOTMART"),
                        null
                ));

        localService.triggerManualRun();

        verify(domainService, times(1)).createCollectionJob(any(MoisWorkspaceDtos.CreateCollectionJobRequest.class));
    }
}

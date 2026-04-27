package com.marketinghub.mois.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoisHotmartCollectionSchedulerTest {

    @Mock
    private MoisModuleGateway gateway;

    private MoisHotmartCollectionProperties properties;
    private MoisHotmartCollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new MoisHotmartCollectionProperties();
        properties.setEnabled(true);
        properties.setWorkspaceId("workspace-001");
        properties.setNiche("marketing-digital");
        properties.setMarketTheme("ofertas-com-temperatura-alta");
        properties.setSources(List.of("HOTMART", " HOTMART ", ""));
        properties.setTimeWindow("LAST_7_DAYS");
        properties.setLimitPerSource(25);
        properties.setLocale("pt-BR");
        properties.setCountry("BR");
        properties.setMinSuccessScore(80);

        scheduler = new MoisHotmartCollectionScheduler(gateway, properties);
    }

    @Test
    void shouldCreateCollectionJobWhenEnabled() {
        when(gateway.createCollectionJob(any())).thenReturn(new MoisWorkspaceDtos.CollectionJobResponse(
                "mois-collect-001",
                "workspace-001",
                "marketing-digital",
                "ofertas-com-temperatura-alta",
                "CREATED",
                "LAST_7_DAYS",
                25,
                80,
                List.of("HOTMART"),
                Instant.parse("2026-04-27T03:10:00Z")
        ));

        scheduler.scheduleCollection();

        ArgumentCaptor<MoisWorkspaceDtos.CreateCollectionJobRequest> captor =
                ArgumentCaptor.forClass(MoisWorkspaceDtos.CreateCollectionJobRequest.class);
        verify(gateway).createCollectionJob(captor.capture());

        MoisWorkspaceDtos.CreateCollectionJobRequest request = captor.getValue();
        assertEquals("workspace-001", request.workspaceId());
        assertEquals("marketing-digital", request.niche());
        assertEquals("LAST_7_DAYS", request.timeWindow());
        assertEquals(List.of("HOTMART"), request.sources());
        assertEquals(80, request.minSuccessScore());
    }

    @Test
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);

        scheduler.scheduleCollection();

        verify(gateway, never()).createCollectionJob(any());
    }

    @Test
    void shouldSkipWhenSourcesAreEmptyAfterSanitization() {
        properties.setSources(List.of(" ", ""));

        scheduler.scheduleCollection();

        verify(gateway, never()).createCollectionJob(any());
    }

    @Test
    void shouldSwallowGatewayFailuresToKeepSchedulerAlive() {
        when(gateway.createCollectionJob(any())).thenThrow(new RuntimeException("gateway down"));

        scheduler.scheduleCollection();

        verify(gateway).createCollectionJob(any());
    }
}

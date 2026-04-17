package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.config.MdsProperties;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdsLoopRunnerTest {

    @Mock
    private BackendMdsClient backendMdsClient;

    @Mock
    private MechanismDiscoveryPipelineService pipelineService;

    @InjectMocks
    private MdsLoopRunner runner;

    @BeforeEach
    void setUp() {
        MdsProperties properties = new MdsProperties();
        properties.setLoopEnabled(true);
        properties.setPollLimit(1);
        properties.getBackend().setWorkerId("mds-worker-test");
        runner = new MdsLoopRunner(backendMdsClient, pipelineService, properties);
    }

    @Test
    void shouldClaimHeartbeatExecuteAndCompleteForPendingRequest() {
        BackendMdsRequestDto request = new BackendMdsRequestDto(
                42L,
                "PENDING",
                "weight-loss",
                "plateau",
                "consistent fat loss",
                "corr-42"
        );
        when(backendMdsClient.getPendingRequests()).thenReturn(List.of(request));

        runner.poll();

        verify(backendMdsClient).claim(eq(42L), any());
        verify(backendMdsClient).heartbeat(eq(42L), any());
        verify(pipelineService).execute(request);
        verify(backendMdsClient).complete(eq(42L), any());
        verify(backendMdsClient, never()).fail(eq(42L), any());
    }

    @Test
    void shouldFailRequestWhenPipelineThrows() {
        BackendMdsRequestDto request = new BackendMdsRequestDto(
                43L,
                "PENDING",
                "metabolic-health",
                "energy crash",
                "stable energy",
                "corr-43"
        );
        when(backendMdsClient.getPendingRequests()).thenReturn(List.of(request));
        doThrow(new RuntimeException("boom")).when(pipelineService).execute(request);

        runner.poll();

        verify(backendMdsClient).claim(eq(43L), any());
        verify(backendMdsClient).heartbeat(eq(43L), any());
        verify(backendMdsClient).fail(eq(43L), any());
        verify(backendMdsClient, never()).complete(eq(43L), any());
    }
}

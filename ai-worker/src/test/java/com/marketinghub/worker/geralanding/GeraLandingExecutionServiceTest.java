package com.marketinghub.worker.geralanding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobCompletionPayload;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineOpenAiClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeraLandingExecutionServiceTest {

    @Test
    void processPendingExecutionsShouldSendPromptToOpenAiAndRegisterResult() {
        GeraLandingBackendClient backendClient = Mockito.mock(GeraLandingBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        ExperimentPipelineOpenAiClient openAiClient = Mockito.mock(ExperimentPipelineOpenAiClient.class);

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(geraLandingService.montarERegistrarPromptEtapa(any(), any())).thenReturn("prompt-content");
        when(openAiClient.generate(any())).thenReturn(new ExperimentPipelineJobCompletionPayload(
                "{\"ok\":true}", "raw", "request", "openai-job", 10, 20, BigDecimal.ONE));

        GeraLandingExecutionService service = new GeraLandingExecutionService(backendClient, geraLandingService, openAiClient, 20);
        service.processPendingExecutions();

        verify(openAiClient).generate(any());
        verify(backendClient).receiveResult(any(), any(), any(), any());
    }
}

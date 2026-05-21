package com.marketinghub.worker.geralanding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

class GeraLandingExecutionServiceTest {

    @Test
    void processPendingExecutionsShouldSendPromptToOpenAiAndRegisterResult() throws Exception {
        GeraLandingBackendClient backendClient = Mockito.mock(GeraLandingBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(geraLandingService.montarERegistrarPromptEtapa(any(), any())).thenReturn("prompt-content");
        when(openAiClient.generate(any())).thenReturn(new GeraLandingJobCompletionPayload(
                "{\"ok\":true}", "raw", "request", "openai-job", 10, 20, BigDecimal.ONE));

        GeraLandingExecutionService service =
                new GeraLandingExecutionService(
                        backendClient,
                        geraLandingService,
                        openAiClient,
                        objectMapper,
                        20,
                        new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(openAiClient).generate(any());
        verify(backendClient).receivePrompt(any(), any(), any(), any(), any(), any(), any(), any());
        verify(backendClient).receiveDispatch(any(), any(), any(), any());
        verify(backendClient).receiveResult(any(), any(), any(), any());
        verify(backendClient, never()).receiveFailure(any(), any(), any(), any(), any());
    }

    @Test
    void processPendingExecutionsShouldRegisterFailureWhenOpenAiFails() throws Exception {
        GeraLandingBackendClient backendClient = Mockito.mock(GeraLandingBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(geraLandingService.montarERegistrarPromptEtapa(any(), any())).thenReturn("prompt-content");
        when(openAiClient.generate(any())).thenThrow(new IllegalStateException("OpenAI erro de flex"));

        GeraLandingExecutionService service =
                new GeraLandingExecutionService(
                        backendClient,
                        geraLandingService,
                        openAiClient,
                        objectMapper,
                        20,
                        new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(backendClient).receiveFailure(any(), any(), any(), any(), any());
        verify(backendClient, never()).receiveResult(any(), any(), any(), any());
    }
}

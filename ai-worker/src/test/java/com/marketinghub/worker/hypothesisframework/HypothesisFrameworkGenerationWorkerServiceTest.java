package com.marketinghub.worker.hypothesisframework;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HypothesisFrameworkGenerationWorkerServiceTest {

    @Mock
    private HypothesisFrameworkBackendClient backendClient;

    @Mock
    private HypothesisFrameworkOpenAiClient openAiClient;

    @Test
    void processPendingMarksJobAsFailedWhenCompletionCallFails() {
        HypothesisFrameworkGenerationWorkerService service =
                new HypothesisFrameworkGenerationWorkerService(backendClient, openAiClient, "worker-test");

        UUID jobId = UUID.randomUUID();
        HypothesisFrameworkJobDto job = new HypothesisFrameworkJobDto(
                jobId,
                UUID.randomUUID(),
                "PAIN",
                "gpt-5.2",
                "prompt",
                "{\"model\":\"gpt-5.2\"}",
                Instant.now());

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPending(20)).thenReturn(List.of(job));
        when(backendClient.claim(jobId, "worker-test")).thenReturn(job);
        when(openAiClient.generate(job)).thenReturn(new HypothesisFrameworkJobCompletionPayload(
                "{\"surface\":\"Dor\"}",
                "{\"raw\":true}",
                "{\"request\":true}",
                10,
                20,
                null));
        doThrow(new IllegalStateException("backend unavailable"))
                .when(backendClient)
                .complete(eq(jobId), any(HypothesisFrameworkJobCompletionPayload.class));

        service.processPending();

        verify(backendClient).fail(jobId, "backend unavailable");
    }

    @Test
    void processPendingSkipsWhenOpenAiDisabled() {
        HypothesisFrameworkGenerationWorkerService service =
                new HypothesisFrameworkGenerationWorkerService(backendClient, openAiClient, "worker-test");
        when(openAiClient.isEnabled()).thenReturn(false);

        service.processPending();

        verify(backendClient, never()).listPending(20);
    }
}

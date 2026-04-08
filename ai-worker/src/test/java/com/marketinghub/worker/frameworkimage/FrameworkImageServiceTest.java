package com.marketinghub.worker.frameworkimage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class FrameworkImageServiceTest {

    @Mock
    private FrameworkImageBackendClient backendClient;

    @Test
    void processPendingClaimsUpdatesStagesAndCompletes() {
        FrameworkImageService service = new FrameworkImageService(backendClient, "worker-test");
        UUID jobId = UUID.randomUUID();
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                jobId,
                10L,
                "hero-1",
                "PENDING",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-1",
                "prompt",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());

        when(backendClient.listPending(20)).thenReturn(List.of(job));
        when(backendClient.claim(jobId, "worker-test")).thenReturn(job);

        service.processPending();

        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.WAITING_OPENAI_BATCH);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.OPENAI_IMAGE_READY);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.UPLOADED_TO_CLOUDFLARE);
        verify(backendClient).complete(eq(jobId), any(FrameworkImageJobCompletionPayload.class));
    }

    @Test
    void processPendingFailsJobWhenStageTransitionThrows() {
        FrameworkImageService service = new FrameworkImageService(backendClient, "worker-test");
        UUID jobId = UUID.randomUUID();
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                jobId,
                10L,
                "hero-1",
                "PENDING",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-1",
                "prompt",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());

        when(backendClient.listPending(20)).thenReturn(List.of(job));
        when(backendClient.claim(jobId, "worker-test")).thenReturn(job);
        org.mockito.Mockito.doThrow(new IllegalStateException("backend unavailable"))
                .when(backendClient)
                .updateStage(jobId, FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);

        service.processPending();

        verify(backendClient).fail(jobId, "backend unavailable");
        verify(backendClient, never()).complete(eq(jobId), any(FrameworkImageJobCompletionPayload.class));
    }

    @Test
    void processPendingSkipsWhenJobCannotBeClaimed() {
        FrameworkImageService service = new FrameworkImageService(backendClient, "worker-test");
        UUID jobId = UUID.randomUUID();
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                jobId,
                10L,
                "hero-1",
                "PENDING",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-1",
                "prompt",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());

        when(backendClient.listPending(20)).thenReturn(List.of(job));
        when(backendClient.claim(jobId, "worker-test")).thenReturn(null);

        service.processPending();

        verify(backendClient, never()).updateStage(eq(jobId), any(FrameworkImageJobStage.class));
        verify(backendClient, never()).complete(eq(jobId), any(FrameworkImageJobCompletionPayload.class));
    }
}

package com.marketinghub.worker.frameworkimage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class FrameworkImageServiceTest {

    @Mock
    private FrameworkImageBackendClient backendClient;

    @Mock
    private FrameworkImageOpenAiBatchClient openAiBatchClient;

    @Mock
    private FrameworkImageStorageClient storageClient;

    @Mock
    private CreativeImageOptimizer imageOptimizer;

    @Test
    void processPendingClaimsRunsBatchAndCompletesWithOpenAiReadyStage() {
        FrameworkImageService service = new FrameworkImageService(
                backendClient,
                openAiBatchClient,
                storageClient,
                imageOptimizer,
                WebClient.builder(),
                3,
                java.time.Duration.ofMillis(10),
                "worker-test",
                true,
                100,
                0d);
        UUID jobId = UUID.randomUUID();
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                jobId,
                10L,
                "hero-1",
                "PENDING",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-2",
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
        when(openAiBatchClient.generateBatch(List.of(job))).thenReturn(Map.of(
                jobId,
                FrameworkImageOpenAiBatchClient.FrameworkImageBatchResult.success(
                        jobId,
                        "batch-123",
                        "gpt-image-2",
                        "prompt",
                        new byte[] {1, 2, 3},
                        null
                )));
        when(imageOptimizer.optimize(any())).thenReturn(new CreativeImageOptimizer.OptimizedImage(new byte[] {1, 2, 3}, "jpg"));
        when(storageClient.upload(any(), any())).thenReturn(
                new FrameworkImageStorageClient.UploadedFrameworkImage("framework-image/test.jpg", "https://cdn.example/framework-image/test.jpg"));

        service.processPending();

        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.WAITING_OPENAI_BATCH);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.OPENAI_IMAGE_READY);
        verify(backendClient).updateStage(jobId, FrameworkImageJobStage.UPLOADED_TO_CLOUDFLARE);
        verify(backendClient).complete(eq(jobId), any(FrameworkImageJobCompletionPayload.class));
    }

    @Test
    void processPendingFailsAllClaimedJobsWhenBatchThrows() {
        FrameworkImageService service = new FrameworkImageService(
                backendClient,
                openAiBatchClient,
                storageClient,
                imageOptimizer,
                WebClient.builder(),
                3,
                java.time.Duration.ofMillis(10),
                "worker-test",
                true,
                100,
                0d);
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
        when(openAiBatchClient.generateBatch(List.of(job))).thenThrow(new IllegalStateException("batch down"));

        service.processPending();

        verify(backendClient).fail(jobId, "batch down");
        verify(backendClient, never()).complete(eq(jobId), any(FrameworkImageJobCompletionPayload.class));
    }

    @Test
    void processPendingSkipsWhenJobCannotBeClaimed() {
        FrameworkImageService service = new FrameworkImageService(
                backendClient,
                openAiBatchClient,
                storageClient,
                imageOptimizer,
                WebClient.builder(),
                3,
                java.time.Duration.ofMillis(10),
                "worker-test",
                true,
                100,
                0d);
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
        verify(openAiBatchClient, never()).generateBatch(any());
    }
}

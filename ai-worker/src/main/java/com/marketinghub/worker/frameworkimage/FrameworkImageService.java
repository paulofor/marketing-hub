package com.marketinghub.worker.frameworkimage;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import java.net.InetAddress;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class FrameworkImageService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageService.class);
    private static final int DEFAULT_UPLOAD_ATTEMPTS = 3;
    private static final Duration DEFAULT_UPLOAD_BACKOFF = Duration.ofMillis(300);

    private final FrameworkImageBackendClient backendClient;
    private final FrameworkImageOpenAiBatchClient openAiBatchClient;
    private final FrameworkImageStorageClient storageClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final WebClient webClient;
    private final String workerId;
    private final int uploadAttempts;
    private final Duration uploadBackoff;

    public FrameworkImageService(FrameworkImageBackendClient backendClient,
                                 FrameworkImageOpenAiBatchClient openAiBatchClient,
                                 FrameworkImageStorageClient storageClient,
                                 CreativeImageOptimizer imageOptimizer,
                                 WebClient.Builder webClientBuilder,
                                 @Value("${framework-image.upload.max-attempts:3}") int uploadAttempts,
                                 @Value("${framework-image.upload.backoff:PT0.3S}") Duration uploadBackoff,
                                 @Value("${worker.id:}") String configuredWorkerId) {
        this.backendClient = backendClient;
        this.openAiBatchClient = openAiBatchClient;
        this.storageClient = storageClient;
        this.imageOptimizer = imageOptimizer;
        this.webClient = webClientBuilder.build();
        this.workerId = resolveWorkerId(configuredWorkerId);
        this.uploadAttempts = uploadAttempts > 0 ? uploadAttempts : DEFAULT_UPLOAD_ATTEMPTS;
        this.uploadBackoff = normalizeDuration(uploadBackoff, DEFAULT_UPLOAD_BACKOFF);
    }

    public void processPending() {
        List<FrameworkImageJobDto> pendingJobs = backendClient.listPending(20);
        if (pendingJobs.isEmpty()) {
            log.debug("Framework image worker found no pending jobs");
            return;
        }

        log.info("Framework image worker found {} pending job(s)", pendingJobs.size());
        Map<UUID, FrameworkImageJobDto> claimedJobs = new LinkedHashMap<>();

        for (FrameworkImageJobDto job : pendingJobs) {
            FrameworkImageJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                log.info("Framework image job {} could not be claimed by worker {}", job.id(), workerId);
                continue;
            }
            claimedJobs.put(claimed.id(), claimed);
            backendClient.updateStage(claimed.id(), FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);
            backendClient.updateStage(claimed.id(), FrameworkImageJobStage.WAITING_OPENAI_BATCH);
        }

        if (claimedJobs.isEmpty()) {
            return;
        }

        try {
            Map<UUID, FrameworkImageOpenAiBatchClient.FrameworkImageBatchResult> batchResults =
                    openAiBatchClient.generateBatch(claimedJobs.values().stream().toList());

            for (FrameworkImageJobDto claimedJob : claimedJobs.values()) {
                FrameworkImageOpenAiBatchClient.FrameworkImageBatchResult result = batchResults.get(claimedJob.id());
                if (result == null || !result.success()) {
                    String reason = result != null && StringUtils.hasText(result.errorMessage())
                            ? result.errorMessage()
                            : "OpenAI batch did not return a valid response for the job";
                    backendClient.fail(claimedJob.id(), reason);
                    continue;
                }

                backendClient.updateStage(claimedJob.id(), FrameworkImageJobStage.OPENAI_IMAGE_READY);
                byte[] imageContent = resolveImageContent(result, claimedJob.id());
                CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageContent);
                FrameworkImageStorageClient.UploadedFrameworkImage uploaded =
                        uploadWithRetry(optimized.content(), buildFilename(claimedJob));
                backendClient.updateStage(claimedJob.id(), FrameworkImageJobStage.UPLOADED_TO_CLOUDFLARE);
                FrameworkImageJobCompletionPayload completionPayload = new FrameworkImageJobCompletionPayload(
                        FrameworkImageJobStage.NOTIFIED_BACKEND.name(),
                        StringUtils.hasText(result.model()) ? result.model() : claimedJob.model(),
                        StringUtils.hasText(result.prompt()) ? result.prompt() : claimedJob.prompt(),
                        result.batchId(),
                        claimedJob.assetId(),
                        uploaded.publicUrl(),
                        claimedJob.webUrl());
                backendClient.complete(claimedJob.id(), completionPayload);
                log.info("Framework image job {} uploaded to Cloudflare and notified backend (batchId={}, objectKey={})",
                        claimedJob.id(), result.batchId(), uploaded.objectKey());
            }
        } catch (Exception ex) {
            String reason = buildFailureReason(ex);
            for (FrameworkImageJobDto claimedJob : claimedJobs.values()) {
                backendClient.fail(claimedJob.id(), reason);
            }
            log.error("Framework image batch processing failed: {}", reason, ex);
        }
    }

    private byte[] resolveImageContent(FrameworkImageOpenAiBatchClient.FrameworkImageBatchResult result, UUID jobId) {
        if (result.imageContent() != null && result.imageContent().length > 0) {
            return result.imageContent();
        }
        if (StringUtils.hasText(result.imageUrl())) {
            byte[] downloaded = webClient.get()
                    .uri(result.imageUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (downloaded != null && downloaded.length > 0) {
                return downloaded;
            }
        }
        throw new IllegalStateException("OpenAI batch did not provide image bytes for job " + jobId);
    }

    private FrameworkImageStorageClient.UploadedFrameworkImage uploadWithRetry(byte[] content, String preferredName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= uploadAttempts; attempt++) {
            try {
                return storageClient.upload(content, preferredName);
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt >= uploadAttempts) {
                    break;
                }
                long backoffMillis = uploadBackoff.toMillis() * attempt;
                log.warn("Framework image upload failed on attempt {}/{}: {}. Retrying in {}ms",
                        attempt, uploadAttempts, ex.getMessage(), backoffMillis);
                sleep(backoffMillis);
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Framework image upload failed");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(50L, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry framework image upload", e);
        }
    }

    private String buildFilename(FrameworkImageJobDto job) {
        String planningItemKey = StringUtils.hasText(job.planningItemKey()) ? job.planningItemKey() : "item";
        String normalized = planningItemKey.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
        return job.id() + "-" + normalized + ".jpg";
    }

    private String resolveWorkerId(String configuredWorkerId) {
        if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
            return configuredWorkerId.trim();
        }
        try {
            return "framework-image-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "framework-image-worker";
        }
    }

    private String buildFailureReason(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Falha desconhecida no processamento do job de imagem";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}

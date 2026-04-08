package com.marketinghub.worker.frameworkimage;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrameworkImageService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageService.class);

    private final FrameworkImageBackendClient backendClient;
    private final FrameworkImageOpenAiBatchClient openAiBatchClient;
    private final String workerId;

    public FrameworkImageService(FrameworkImageBackendClient backendClient,
                                 FrameworkImageOpenAiBatchClient openAiBatchClient,
                                 @Value("${worker.id:}") String configuredWorkerId) {
        this.backendClient = backendClient;
        this.openAiBatchClient = openAiBatchClient;
        this.workerId = resolveWorkerId(configuredWorkerId);
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
                FrameworkImageJobCompletionPayload completionPayload = new FrameworkImageJobCompletionPayload(
                        FrameworkImageJobStage.OPENAI_IMAGE_READY.name(),
                        StringUtils.hasText(result.model()) ? result.model() : claimedJob.model(),
                        StringUtils.hasText(result.prompt()) ? result.prompt() : claimedJob.prompt(),
                        result.batchId(),
                        claimedJob.assetId(),
                        claimedJob.sourceUrl(),
                        claimedJob.webUrl());
                backendClient.complete(claimedJob.id(), completionPayload);
                log.info("Framework image job {} reached OPENAI_IMAGE_READY (batchId={})",
                        claimedJob.id(), result.batchId());
            }
        } catch (Exception ex) {
            String reason = buildFailureReason(ex);
            for (FrameworkImageJobDto claimedJob : claimedJobs.values()) {
                backendClient.fail(claimedJob.id(), reason);
            }
            log.error("Framework image batch processing failed: {}", reason, ex);
        }
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
}

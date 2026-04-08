package com.marketinghub.worker.frameworkimage;

import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FrameworkImageService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageService.class);

    private final FrameworkImageBackendClient backendClient;
    private final String workerId;

    public FrameworkImageService(FrameworkImageBackendClient backendClient,
                                 @Value("${worker.id:}") String configuredWorkerId) {
        this.backendClient = backendClient;
        this.workerId = resolveWorkerId(configuredWorkerId);
    }

    public void processPending() {
        List<FrameworkImageJobDto> pendingJobs = backendClient.listPending(20);
        if (pendingJobs.isEmpty()) {
            log.debug("Framework image worker found no pending jobs");
            return;
        }

        log.info("Framework image worker found {} pending job(s)", pendingJobs.size());
        for (FrameworkImageJobDto job : pendingJobs) {
            FrameworkImageJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                log.info("Framework image job {} could not be claimed by worker {}", job.id(), workerId);
                continue;
            }

            try {
                log.info("Processing framework image job {} for experiment {}", claimed.id(), claimed.experimentId());
                backendClient.updateStage(claimed.id(), FrameworkImageJobStage.SENT_TO_OPENAI_BATCH);
                backendClient.updateStage(claimed.id(), FrameworkImageJobStage.WAITING_OPENAI_BATCH);
                backendClient.updateStage(claimed.id(), FrameworkImageJobStage.OPENAI_IMAGE_READY);
                backendClient.updateStage(claimed.id(), FrameworkImageJobStage.UPLOADED_TO_CLOUDFLARE);

                FrameworkImageJobCompletionPayload payload = new FrameworkImageJobCompletionPayload(
                        FrameworkImageJobStage.NOTIFIED_BACKEND.name(),
                        claimed.model(),
                        claimed.prompt(),
                        claimed.batchId(),
                        claimed.assetId(),
                        claimed.sourceUrl(),
                        claimed.webUrl());
                backendClient.complete(claimed.id(), payload);
                log.info("Framework image job {} marked as completed", claimed.id());
            } catch (Exception ex) {
                String reason = buildFailureReason(ex);
                backendClient.fail(claimed.id(), reason);
                log.error("Framework image job {} failed: {}", claimed.id(), reason, ex);
            }
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

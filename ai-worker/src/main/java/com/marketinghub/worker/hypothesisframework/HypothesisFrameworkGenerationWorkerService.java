package com.marketinghub.worker.hypothesisframework;

import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HypothesisFrameworkGenerationWorkerService {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkGenerationWorkerService.class);

    private final HypothesisFrameworkBackendClient backendClient;
    private final HypothesisFrameworkOpenAiClient openAiClient;
    private final String workerId;

    public HypothesisFrameworkGenerationWorkerService(HypothesisFrameworkBackendClient backendClient,
                                                      HypothesisFrameworkOpenAiClient openAiClient,
                                                      @Value("${worker.id:}") String configuredWorkerId) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
        this.workerId = resolveWorkerId(configuredWorkerId);
    }

    public void processPending() {
        if (!openAiClient.isEnabled()) {
            return;
        }
        List<HypothesisFrameworkJobDto> pending = backendClient.listPending(20);
        if (pending.isEmpty()) {
            return;
        }
        for (HypothesisFrameworkJobDto job : pending) {
            HypothesisFrameworkJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                continue;
            }
            try {
                HypothesisFrameworkJobCompletionPayload payload = openAiClient.generate(claimed);
                backendClient.complete(claimed.id(), payload);
                log.info("Hypothesis framework job {} completed", claimed.id());
            } catch (Exception ex) {
                String error = ex.getMessage() != null ? ex.getMessage() : "Falha desconhecida";
                backendClient.fail(claimed.id(), error);
                log.error("Hypothesis framework job {} failed", claimed.id(), ex);
            }
        }
    }

    private String resolveWorkerId(String configuredWorkerId) {
        if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
            return configuredWorkerId.trim();
        }
        try {
            return "hypothesis-framework-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "hypothesis-framework-worker";
        }
    }
}

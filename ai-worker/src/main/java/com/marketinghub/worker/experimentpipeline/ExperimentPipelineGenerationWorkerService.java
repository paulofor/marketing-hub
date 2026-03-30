package com.marketinghub.worker.experimentpipeline;

import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExperimentPipelineGenerationWorkerService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineGenerationWorkerService.class);

    private final ExperimentPipelineBackendClient backendClient;
    private final ExperimentPipelineOpenAiClient openAiClient;
    private final String workerId;

    public ExperimentPipelineGenerationWorkerService(ExperimentPipelineBackendClient backendClient,
                                                     ExperimentPipelineOpenAiClient openAiClient,
                                                     @Value("${worker.id:}") String configuredWorkerId) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
        this.workerId = resolveWorkerId(configuredWorkerId);
    }

    public void processPending() {
        if (!openAiClient.isEnabled()) {
            log.warn("Experiment pipeline generation skipped: OpenAI client is disabled");
            return;
        }
        List<ExperimentPipelineJobDto> pending = backendClient.listPending(20);
        if (pending.isEmpty()) {
            return;
        }
        for (ExperimentPipelineJobDto job : pending) {
            ExperimentPipelineJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                continue;
            }
            try {
                backendClient.updateStage(claimed.id(), "SENT_TO_OPENAI");
                backendClient.updateStage(claimed.id(), "WAITING_OPENAI");
                ExperimentPipelineJobCompletionPayload payload = openAiClient.generate(claimed);
                backendClient.complete(claimed.id(), payload);
            } catch (Exception ex) {
                String error = ex.getMessage() != null ? ex.getMessage() : "Falha desconhecida";
                backendClient.fail(claimed.id(), error);
                log.error("Experiment pipeline job {} failed", claimed.id(), ex);
            }
        }
    }

    private String resolveWorkerId(String configuredWorkerId) {
        if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
            return configuredWorkerId.trim();
        }
        try {
            return "experiment-pipeline-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "experiment-pipeline-worker";
        }
    }
}

package com.marketinghub.worker.hypothesisframework;

import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
            log.warn("Hypothesis framework generation skipped: OpenAI client is disabled");
            return;
        }
        log.info("Hypothesis framework generation polling pending jobs (workerId={})", workerId);
        List<HypothesisFrameworkJobDto> pending = backendClient.listPending(20);
        log.info("Hypothesis framework generation fetched {} pending job(s)", pending.size());
        if (pending.isEmpty()) {
            log.debug("No pending hypothesis framework jobs found");
            return;
        }
        for (HypothesisFrameworkJobDto job : pending) {
            log.info("Attempting to claim hypothesis framework job {} (experimentId={})", job.id(), job.experimentId());
            HypothesisFrameworkJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                log.info("Hypothesis framework job {} was not claimed (possibly claimed by another worker)", job.id());
                continue;
            }
            try {
                log.info("Generating hypothesis framework for job {}", claimed.id());
                log.info("OpenAI request payload [jobId={}, hypothesisId={}, section={}, model={}]: {}",
                        claimed.id(),
                        claimed.hypothesisId(),
                        claimed.section(),
                        claimed.model(),
                        truncate(claimed.requestBodyJson()));
                HypothesisFrameworkJobCompletionPayload payload = openAiClient.generate(claimed);
                log.info("OpenAI response payload [jobId={}, hypothesisId={}, section={}, model={}]: {}",
                        claimed.id(),
                        claimed.hypothesisId(),
                        claimed.section(),
                        claimed.model(),
                        truncate(payload != null ? payload.rawResponse() : null));
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

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "<vazio>";
        }
        int maxLength = 3_000;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }
}

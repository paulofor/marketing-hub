package com.marketinghub.worker.experimentpipeline;

import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
        log.info("Experiment pipeline worker cycle started");
        if (!openAiClient.isEnabled()) {
            log.warn("Experiment pipeline generation skipped: OpenAI client is disabled");
            return;
        }
        List<ExperimentPipelineJobDto> pending = backendClient.listPending(20);
        log.info("Experiment pipeline worker found {} pending job(s)", pending.size());
        if (pending.isEmpty()) {
            return;
        }
        for (ExperimentPipelineJobDto job : pending) {
            log.info("Attempting to claim experiment pipeline job {} (experimentId={}, section={})",
                    job.id(), job.experimentId(), job.section());
            ExperimentPipelineJobDto claimed = backendClient.claim(job.id(), workerId);
            if (claimed == null) {
                log.info("Job {} could not be claimed by worker {}", job.id(), workerId);
                continue;
            }
            try {
                log.info("Job {} claimed by worker {}. Sending to OpenAI (section={})",
                        claimed.id(), workerId, claimed.section());
                backendClient.updateStage(claimed.id(), "SENT_TO_OPENAI");
                backendClient.updateStage(claimed.id(), "WAITING_OPENAI");
                ExperimentPipelineJobCompletionPayload payload = openAiClient.generate(claimed);
                log.info("Job {} received OpenAI output; completing job in backend", claimed.id());
                backendClient.complete(claimed.id(), payload);
                log.info("Job {} completed successfully", claimed.id());
            } catch (Exception ex) {
                String error = buildFailureReason(ex);
                backendClient.fail(claimed.id(), error);
                log.error("Experiment pipeline job {} failed", claimed.id(), ex);
            }
        }
        log.info("Experiment pipeline worker cycle finished");
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

    private String buildFailureReason(Exception ex) {
        WebClientResponseException httpError = findHttpError(ex);
        if (httpError != null) {
            HttpStatus status = HttpStatus.resolve(httpError.getStatusCode().value());
            if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
                return "Rejeitado pelo backend ao completar o job (422). Motivo: "
                        + summarizeHttpErrorBody(httpError.getResponseBodyAsString());
            }
            if (status == HttpStatus.BAD_GATEWAY
                    || status == HttpStatus.SERVICE_UNAVAILABLE
                    || status == HttpStatus.GATEWAY_TIMEOUT
                    || status == HttpStatus.TOO_MANY_REQUESTS) {
                return "OpenAI indisponível temporariamente (" + httpError.getStatusCode().value()
                        + "). Tente novamente em alguns minutos.";
            }
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Falha desconhecida";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String summarizeHttpErrorBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "backend não retornou detalhes";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) + "..." : compact;
    }

    private WebClientResponseException findHttpError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebClientResponseException responseException) {
                return responseException;
            }
            current = current.getCause();
        }
        return null;
    }
}

package com.marketinghub.worker.geralanding.deliverables.request;

import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.GeraLandingOpenAiFlexClient;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingDeliverablesBackendClient;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingExperimentDeliverablesRequest;
import com.marketinghub.worker.geralanding.deliverables.GeraLandingJobCompletionDeliverablesPayload;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.deliverables.RecebeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar jobs OpenAI da etapa deliverables de forma isolada no pacote da etapa. */
@Service
public class GeraLandingDeliverablesOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingDeliverablesOpenAiExecutionService.class);
    private final GeraLandingDeliverablesBackendClient backendClient;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final com.marketinghub.worker.geralanding.deliverables.MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    public GeraLandingDeliverablesOpenAiExecutionService(GeraLandingDeliverablesBackendClient backendClient, GeraLandingOpenAiFlexClient openAiClient, com.marketinghub.worker.geralanding.deliverables.MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient; this.openAiClient = openAiClient; this.montaRequest = montaRequest; this.recebeResponse = recebeResponse;
    }
    /** Processa os jobs pendentes da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) { if (!openAiClient.isEnabled()) return; for (GeraLandingStageExecutionDetailDto e : jobs) processExecution(e); }
    /** Processa um job individual da etapa deliverables. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentDeliverablesRequest requestData = new GeraLandingExperimentDeliverablesRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData); String requestBody = montaRequest.montar(requestData);
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), "gpt-5.2", requestBody, prompt, null);
            var basePayload = openAiClient.generate(openAiJob);
            GeraLandingJobCompletionDeliverablesPayload pl = new GeraLandingJobCompletionDeliverablesPayload(basePayload.responseContent(), basePayload.rawResponse(), basePayload.requestBodyJson(), basePayload.openAiJobId(), basePayload.inputTokens(), basePayload.outputTokens(), basePayload.costUsd());
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), pl);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa deliverables para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}

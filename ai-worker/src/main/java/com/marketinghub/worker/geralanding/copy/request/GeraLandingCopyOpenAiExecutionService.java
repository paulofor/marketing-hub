package com.marketinghub.worker.geralanding.copy.request;

import com.marketinghub.worker.geralanding.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.GeraLandingOpenAiFlexClient;
import com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient;
import com.marketinghub.worker.geralanding.copy.GeraLandingExperimentRequest;
import com.marketinghub.worker.geralanding.copy.GeraLandingJobCompletionPayload;
import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.copy.RecebeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar jobs OpenAI da etapa copy de forma isolada no pacote da etapa. */
@Service
public class GeraLandingCopyOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingCopyOpenAiExecutionService.class);
    private final GeraLandingCopyBackendClient backendClient;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final com.marketinghub.worker.geralanding.copy.MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;

    public GeraLandingCopyOpenAiExecutionService(GeraLandingCopyBackendClient backendClient, GeraLandingOpenAiFlexClient openAiClient, com.marketinghub.worker.geralanding.copy.MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
        this.montaRequest = montaRequest;
        this.recebeResponse = recebeResponse;
    }

    /** Processa os jobs pendentes da etapa copy. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        if (!openAiClient.isEnabled()) return;
        for (GeraLandingStageExecutionDetailDto execution : jobs) processExecution(execution);
    }

    /** Processa um job individual da etapa copy. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentRequest requestData = new GeraLandingExperimentRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData);
            String requestBody = montaRequest.montar(requestData);
            String openAiModel = "gpt-5.2";
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), openAiModel, requestBody, prompt, null);
            var payloadBase = openAiClient.generate(openAiJob);
            GeraLandingJobCompletionPayload payload = new GeraLandingJobCompletionPayload(payloadBase.responseContent(), payloadBase.rawResponse(), payloadBase.requestBodyJson(), payloadBase.openAiJobId(), payloadBase.inputTokens(), payloadBase.outputTokens(), payloadBase.costUsd());
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa copy para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}

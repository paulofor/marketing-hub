package com.marketinghub.worker.geralanding.imageplanning.request;

import com.marketinghub.worker.geralanding.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.GeraLandingOpenAiFlexClient;
import com.marketinghub.worker.geralanding.imageplanning.backend.GeraLandingImagePlanningBackendClient;
import com.marketinghub.worker.geralanding.imageplanning.GeraLandingExperimentImagePlanningRequest;
import com.marketinghub.worker.geralanding.imageplanning.GeraLandingJobCompletionImagePlanningPayload;
import com.marketinghub.worker.geralanding.imageplanning.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.imageplanning.response.RecebeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar jobs OpenAI da etapa imageplanning de forma isolada no pacote da etapa. */
@Service
public class GeraLandingImagePlanningOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingImagePlanningOpenAiExecutionService.class);
    private final GeraLandingImagePlanningBackendClient backendClient;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final com.marketinghub.worker.geralanding.imageplanning.request.MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    public GeraLandingImagePlanningOpenAiExecutionService(GeraLandingImagePlanningBackendClient backendClient, GeraLandingOpenAiFlexClient openAiClient, com.marketinghub.worker.geralanding.imageplanning.request.MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient; this.openAiClient = openAiClient; this.montaRequest = montaRequest; this.recebeResponse = recebeResponse;
    }
    /** Processa os jobs pendentes da etapa imageplanning. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) { if (!openAiClient.isEnabled()) return; for (GeraLandingStageExecutionDetailDto e : jobs) processExecution(e); }
    /** Processa um job individual da etapa imageplanning. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentImagePlanningRequest requestData = new GeraLandingExperimentImagePlanningRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData); String requestBody = montaRequest.montar(requestData);
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), "gpt-5.2", requestBody, prompt, null);
            var basePayload = openAiClient.generate(openAiJob);
            GeraLandingJobCompletionImagePlanningPayload pl = new GeraLandingJobCompletionImagePlanningPayload(basePayload.responseContent(), basePayload.rawResponse(), basePayload.requestBodyJson(), basePayload.openAiJobId(), basePayload.inputTokens(), basePayload.outputTokens(), basePayload.costUsd());
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), pl);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa imageplanning para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}

package com.marketinghub.worker.geralanding.wireframe.request;

import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.GeraLandingOpenAiFlexClient;
import com.marketinghub.worker.geralanding.wireframe.backend.GeraLandingWireframeBackendClient;
import com.marketinghub.worker.geralanding.wireframe.response.GeraLandingJobCompletionWireframePayload;
import com.marketinghub.worker.geralanding.wireframe.response.RecebeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar jobs OpenAI da etapa wireframe de forma isolada no pacote da etapa. */
@Service
public class GeraLandingWireframeOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingWireframeOpenAiExecutionService.class);
    private final GeraLandingWireframeBackendClient backendClient;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    public GeraLandingWireframeOpenAiExecutionService(GeraLandingWireframeBackendClient backendClient, GeraLandingOpenAiFlexClient openAiClient, MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient; this.openAiClient = openAiClient; this.montaRequest = montaRequest; this.recebeResponse = recebeResponse;
    }
    /** Processa os jobs pendentes da etapa wireframe. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) { if (!openAiClient.isEnabled()) return; for (GeraLandingStageExecutionDetailDto e: jobs) processExecution(e); }
    /** Processa um job individual da etapa wireframe. */
    public void processExecution(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String,Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentWireframeRequest requestData = new GeraLandingExperimentWireframeRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData); String requestBody = montaRequest.montar(requestData);
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), "gpt-5.2", requestBody, prompt, null);
            var basePayload = openAiClient.generate(openAiJob);
            GeraLandingJobCompletionWireframePayload pl = new GeraLandingJobCompletionWireframePayload(basePayload.responseContent(), basePayload.rawResponse(), basePayload.requestBodyJson(), basePayload.openAiJobId(), basePayload.inputTokens(), basePayload.outputTokens(), basePayload.costUsd());
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), pl);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa wireframe para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}

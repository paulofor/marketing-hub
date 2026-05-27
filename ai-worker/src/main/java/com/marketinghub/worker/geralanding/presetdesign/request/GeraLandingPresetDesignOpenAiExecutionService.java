package com.marketinghub.worker.geralanding.presetdesign.request;

import com.marketinghub.worker.geralanding.GeraLandingJobDto;
import com.marketinghub.worker.geralanding.GeraLandingOpenAiFlexClient;
import com.marketinghub.worker.geralanding.presetdesign.GeraLandingPresetDesignBackendClient;
import com.marketinghub.worker.geralanding.presetdesign.GeraLandingExperimentPresetDesignRequest;
import com.marketinghub.worker.geralanding.presetdesign.GeraLandingJobCompletionPresetDesignPayload;
import com.marketinghub.worker.geralanding.presetdesign.dto.GeraLandingStageExecutionPresetDesignDto;
import com.marketinghub.worker.geralanding.presetdesign.RecebeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar jobs OpenAI da etapa presetdesign de forma isolada no pacote da etapa. */
@Service
public class GeraLandingPresetDesignOpenAiExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingPresetDesignOpenAiExecutionService.class);
    private final GeraLandingPresetDesignBackendClient backendClient;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final com.marketinghub.worker.geralanding.presetdesign.MontaRequest montaRequest;
    private final RecebeResponse recebeResponse;
    public GeraLandingPresetDesignOpenAiExecutionService(GeraLandingPresetDesignBackendClient backendClient, GeraLandingOpenAiFlexClient openAiClient, com.marketinghub.worker.geralanding.presetdesign.MontaRequest montaRequest, RecebeResponse recebeResponse) {
        this.backendClient = backendClient; this.openAiClient = openAiClient; this.montaRequest = montaRequest; this.recebeResponse = recebeResponse;
    }
    /** Processa os jobs pendentes da etapa presetdesign. */
    public void processExecutions(List<GeraLandingStageExecutionPresetDesignDto> jobs) { if (!openAiClient.isEnabled()) return; for (GeraLandingStageExecutionPresetDesignDto e : jobs) processExecution(e); }
    /** Processa um job individual da etapa presetdesign. */
    public void processExecution(GeraLandingStageExecutionPresetDesignDto execution) {
        if (execution == null || !StringUtils.hasText(execution.idJob())) return;
        try {
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentPresetDesignRequest requestData = new GeraLandingExperimentPresetDesignRequest(execution.experimentId(), dadosPrompt);
            String prompt = montaRequest.montarPrompt(requestData); String requestBody = montaRequest.montar(requestData);
            GeraLandingJobDto openAiJob = new GeraLandingJobDto(UUID.fromString(execution.idJob()), execution.experimentId(), execution.stageCode(), "gpt-5.2", requestBody, prompt, null);
            var basePayload = openAiClient.generate(openAiJob);
            GeraLandingJobCompletionPresetDesignPayload pl = new GeraLandingJobCompletionPresetDesignPayload(basePayload.responseContent(), basePayload.rawResponse(), basePayload.requestBodyJson(), basePayload.openAiJobId(), basePayload.inputTokens(), basePayload.outputTokens(), basePayload.costUsd());
            recebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), pl);
        } catch (Exception ex) {
            log.error("Falha ao processar etapa presetdesign para executionId={} (experimentId={})", execution.idJob(), execution.experimentId(), ex);
            backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage(), ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}

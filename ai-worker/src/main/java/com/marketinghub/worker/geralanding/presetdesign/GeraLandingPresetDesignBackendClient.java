package com.marketinghub.worker.geralanding.presetdesign;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa presetdesign. */
@Component
public class GeraLandingPresetDesignBackendClient {
    private final com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient;

    public GeraLandingPresetDesignBackendClient(com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionPresetDesignDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(GeraLandingStageExecutionPresetDesignDto::fromBase).toList();
    }

    /** Envia resultado da etapa para o backend principal com mapeamento para payload base. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionPresetDesignPayload payload) {
        backendClient.receiveResult(idJob, experimentId, stageCode,
                new com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload(
                        payload != null ? payload.responseContent() : null,
                        payload != null ? payload.rawResponse() : null,
                        payload != null ? payload.requestBodyJson() : null,
                        payload != null ? payload.openAiJobId() : null,
                        payload != null ? payload.inputTokens() : null,
                        payload != null ? payload.outputTokens() : null,
                        payload != null ? payload.costUsd() : null));
    }


    /** Encaminha confirmação de despacho da etapa para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        backendClient.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchDesignPresetStageExecutionDetail(experimentId, idJob);
    }
}

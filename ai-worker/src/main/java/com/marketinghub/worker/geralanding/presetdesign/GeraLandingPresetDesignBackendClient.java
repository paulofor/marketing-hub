package com.marketinghub.worker.geralanding.presetdesign;

import com.marketinghub.worker.geralanding.presetdesign.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa presetdesign. */
@Component
public class GeraLandingPresetDesignBackendClient {
    private final com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient;

    public GeraLandingPresetDesignBackendClient(com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(item -> new GeraLandingStageExecutionDetailDto(item.experimentId(), item.stageCode(), item.idJob(), item.status(), item.executionRequestedAt(), item.processingStartedAt(), item.completedAt(), item.openAiJobId())).toList();
    }

    /** Envia resultado da etapa para o backend principal com mapeamento para payload base. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionPresetDesignPayload payload) {
        backendClient.receiveResult(idJob, experimentId, stageCode, payload);
    }


    /** Encaminha confirmação de despacho da etapa para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        backendClient.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }


    /** Carrega os dados de prompt necessários para execução da etapa. */
    public java.util.Map<String, Object> loadPromptData(Long experimentId) {
        return backendClient.loadPromptData(experimentId);
    }

    /** Encaminha falha de execução da etapa para o backend principal. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        backendClient.receiveFailure(idJob, experimentId, stageCode, errorMessage, errorDetail);
    }
    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchDesignPresetStageExecutionDetail(experimentId, idJob);
    }
}

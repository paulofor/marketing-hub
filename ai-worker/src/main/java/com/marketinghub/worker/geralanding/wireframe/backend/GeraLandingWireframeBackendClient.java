package com.marketinghub.worker.geralanding.wireframe.backend;

import com.marketinghub.worker.geralanding.wireframe.response.GeraLandingJobCompletionWireframePayload;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa wireframe. */
@Component
public class GeraLandingWireframeBackendClient {
    private final com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient;

    public GeraLandingWireframeBackendClient(com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa wireframe. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream()
                .map(item -> new GeraLandingStageExecutionDetailDto(item.experimentId(), item.stageCode(), item.idJob(), item.status(), item.executionRequestedAt(), item.processingStartedAt(), item.completedAt(), item.openAiJobId()))
                .toList();
    }


    /** Carrega os dados de prompt necessários para execução da etapa. */
    public java.util.Map<String, Object> loadPromptData(Long experimentId) {
        return backendClient.loadPromptData(experimentId);
    }

    /** Encaminha falha de execução da etapa para o backend principal. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        backendClient.receiveFailure(idJob, experimentId, stageCode, errorMessage, errorDetail);
    }
    /** Busca os detalhes de execução da etapa wireframe no backend. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        backendClient.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Envia resultado da etapa wireframe ao backend principal. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionWireframePayload payload) {
        backendClient.receiveResult(idJob, experimentId, stageCode, payload);
    }

    /** Busca os detalhes de execução da etapa wireframe no backend. */
    public GeraLandingStageExecutionDetailDto fetchWireframeStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchWireframeStageExecutionDetail(experimentId, idJob);
    }
}

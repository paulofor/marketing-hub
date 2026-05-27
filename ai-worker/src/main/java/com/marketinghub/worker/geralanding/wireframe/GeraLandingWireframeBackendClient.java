package com.marketinghub.worker.geralanding.wireframe;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa wireframe. */
@Component
public class GeraLandingWireframeBackendClient {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingComumBackendClient backendClient;

    public GeraLandingWireframeBackendClient(com.marketinghub.worker.geralanding.comum.GeraLandingComumBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa wireframe. */
    public List<GeraLandingStageExecutionWireframeDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream()
                .map(item -> new GeraLandingStageExecutionWireframeDto(item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
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

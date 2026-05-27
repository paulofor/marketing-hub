package com.marketinghub.worker.geralanding.imageplanning;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa imageplanning. */
@Component
public class GeraLandingImagePlanningBackendClient {
    private final com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient;

    public GeraLandingImagePlanningBackendClient(com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionImagePlanningDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(GeraLandingStageExecutionImagePlanningDto::fromBase).toList();
    }

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchImagePlanningStageExecutionDetail(experimentId, idJob);
    }
}

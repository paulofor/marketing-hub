package com.marketinghub.worker.geralanding.deliverables;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa deliverables. */
@Component
public class GeraLandingDeliverablesBackendClient {
    private final com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient;

    public GeraLandingDeliverablesBackendClient(com.marketinghub.worker.geralanding.GeraLandingBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionDeliverablesDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(GeraLandingStageExecutionDeliverablesDto::fromBase).toList();
    }

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchDeliverablesStageExecutionDetail(experimentId, idJob);
    }
}

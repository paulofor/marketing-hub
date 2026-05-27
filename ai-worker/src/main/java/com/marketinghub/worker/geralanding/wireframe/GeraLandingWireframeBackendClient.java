package com.marketinghub.worker.geralanding.wireframe;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa wireframe. */
@Component
@Deprecated
public class GeraLandingWireframeBackendClient {
    private final GeraLandingBackendClient backendClient;

    public GeraLandingWireframeBackendClient(GeraLandingBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa wireframe. */
    public List<GeraLandingStageExecutionWireframeDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream()
                .map(GeraLandingStageExecutionWireframeDto::fromBase)
                .toList();
    }

    /** Busca os detalhes de execução da etapa wireframe no backend. */
    public GeraLandingStageExecutionDetailDto fetchWireframeStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchWireframeStageExecutionDetail(experimentId, idJob);
    }
}

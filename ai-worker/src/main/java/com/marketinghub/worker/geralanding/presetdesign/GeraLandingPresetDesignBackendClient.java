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

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchDesignPresetStageExecutionDetail(experimentId, idJob);
    }
}

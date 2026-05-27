package com.marketinghub.worker.geralanding.imageplanning;

import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa imageplanning. */
@Component
public class GeraLandingImagePlanningBackendClient {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingComumBackendClient backendClient;

    public GeraLandingImagePlanningBackendClient(com.marketinghub.worker.geralanding.comum.GeraLandingComumBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionImagePlanningDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(GeraLandingStageExecutionImagePlanningDto::fromBase).toList();
    }

    /** Envia resultado da etapa para o backend principal com mapeamento para payload base. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionImagePlanningPayload payload) {
        backendClient.receiveResult(idJob, experimentId, stageCode, payload);
    }


    /** Encaminha confirmação de despacho da etapa para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        backendClient.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchImagePlanningStageExecutionDetail(experimentId, idJob);
    }
}

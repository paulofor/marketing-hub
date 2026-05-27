package com.marketinghub.worker.geralanding.deliverables;

import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDeliverablesDto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao backend para operações da etapa deliverables. */
@Component
public class GeraLandingDeliverablesBackendClient {
    private final com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient;

    public GeraLandingDeliverablesBackendClient(com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Lista execuções pendentes convertendo para o DTO específico da etapa. */
    public List<GeraLandingStageExecutionDeliverablesDto> listPendingExecutions(int limit) {
        return backendClient.listPendingExecutions(limit).stream().map(GeraLandingStageExecutionDeliverablesDto::fromBase).toList();
    }

    /** Envia resultado da etapa para o backend principal com mapeamento para payload base. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionDeliverablesPayload payload) {
        backendClient.receiveResult(idJob, experimentId, stageCode, payload);
    }


    /** Encaminha confirmação de despacho da etapa para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        backendClient.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Busca os detalhes de execução da etapa no backend. */
    public GeraLandingStageExecutionDetailDto fetchStageExecutionDetail(Long experimentId, String idJob) {
        return backendClient.fetchDeliverablesStageExecutionDetail(experimentId, idJob);
    }
}

package com.marketinghub.worker.geralanding.wireframe;

import java.util.List;
import org.springframework.stereotype.Component;

/** Responsabilidade: encapsular o cliente de backend da etapa wireframe mantendo isolamento por pacote. */
@Component("geraLandingWireframeBackendClient")
public class GeraLandingBackendClient {
    private final com.marketinghub.worker.geralanding.GeraLandingBackendClient delegate;

    public GeraLandingBackendClient(com.marketinghub.worker.geralanding.GeraLandingBackendClient delegate) {
        this.delegate = delegate;
    }

    /** Lista execuções pendentes e converte para o DTO local da etapa wireframe. */
    public List<GeraLandingStageExecutionWireframeDto> listPendingExecutions(int limit) {
        return delegate.listPendingExecutions(limit).stream()
                                .map(item -> new GeraLandingStageExecutionWireframeDto(
                        item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
    }

    /** Encaminha confirmação de despacho do job da etapa wireframe para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        delegate.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Envia resultado da etapa wireframe ao backend principal. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionWireframePayload payload) {
        delegate.receiveResult(idJob, experimentId, stageCode, payload.toBase());
    }

    /** Busca detalhes da execução da etapa wireframe no endpoint dedicado da etapa. */
    public GeraLandingStageExecutionDetailDto fetchWireframeStageExecutionDetail(Long experimentId, String idJob) {
        return delegate.fetchWireframeStageExecutionDetail(experimentId, idJob);
    }
}

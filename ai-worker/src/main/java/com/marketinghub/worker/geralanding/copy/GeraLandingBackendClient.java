package com.marketinghub.worker.geralanding.copy;

import java.util.List;
import org.springframework.stereotype.Component;

/** Responsabilidade: encapsular o cliente de backend da etapa copy mantendo isolamento por pacote. */
@Component("geraLandingCopyBackendClient")
public class GeraLandingBackendClient {
    private final com.marketinghub.worker.geralanding.GeraLandingBackendClient delegate;

    public GeraLandingBackendClient(com.marketinghub.worker.geralanding.GeraLandingBackendClient delegate) {
        this.delegate = delegate;
    }

    /** Lista execuções pendentes e converte para o DTO local da etapa copy. */
    public List<GeraLandingStageExecutionDto> listPendingExecutions(int limit) {
        return delegate.listPendingExecutions(limit).stream()
                .map(item -> new GeraLandingStageExecutionDto(item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
    }

    /** Encaminha confirmação de despacho do job da etapa copy para o backend principal. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        delegate.receiveDispatch(idJob, experimentId, stageCode, openAiJobId);
    }

    /** Envia resultado da etapa copy ao backend principal. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionPayload payload) {
        delegate.receiveResult(idJob, experimentId, stageCode,
                new com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload(
                        payload.responseContent(),
                        payload.rawResponse(),
                        payload.requestBodyJson(),
                        payload.openAiJobId(),
                        payload.inputTokens(),
                        payload.outputTokens(),
                        payload.costUsd()));
    }

    /** Busca detalhes da execução da etapa copy no endpoint dedicado da etapa. */
    public GeraLandingStageExecutionDetailDto fetchCopyStageExecutionDetail(Long experimentId, String idJob) {
        return delegate.fetchCopyStageExecutionDetail(experimentId, idJob);
    }
}

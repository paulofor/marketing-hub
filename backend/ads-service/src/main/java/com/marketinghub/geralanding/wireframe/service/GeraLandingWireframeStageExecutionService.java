package com.marketinghub.geralanding.wireframe.service;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsável por adaptar consultas de execução para o contrato da etapa wireframe. */
@Service
public class GeraLandingWireframeStageExecutionService {

    private final GeraLandingStageExecutionService delegate;

    public GeraLandingWireframeStageExecutionService(GeraLandingStageExecutionService delegate) {
        this.delegate = delegate;
    }

    /** Lista execuções da etapa convertendo para o DTO local da etapa. */
    public List<GeraLandingWireframeExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, String stageCode, boolean includeCompleted) {
        return delegate.listExperimentStageExecutions(experimentId, stageCode, includeCompleted).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Retorna o detalhe da execução convertido para o DTO local da etapa. */
    public GeraLandingWireframeStageExecutionDetailResponse getStageExecutionDetail(Long experimentId, String idJob) {
        return toDetailResponse(delegate.getStageExecutionDetail(experimentId, idJob));
    }

    /** Converte o resumo transversal para o resumo local da etapa. */
    private GeraLandingWireframeExecutionSummaryResponse toSummaryResponse(GeraLandingExecutionSummaryResponse response) {
        return new GeraLandingWireframeExecutionSummaryResponse(
                response.idJob(),
                response.status(),
                response.executionRequestedAt(),
                response.costUsd());
    }

    /** Converte o detalhe transversal para o detalhe local da etapa. */
    private GeraLandingWireframeStageExecutionDetailResponse toDetailResponse(GeraLandingStageExecutionDetailResponse response) {
        return new GeraLandingWireframeStageExecutionDetailResponse(
                response.idJob(),
                response.experimentId(),
                response.stageCode(),
                response.executionRequestedAt(),
                response.createdAt(),
                response.processingStartedAt(),
                response.completedAt(),
                response.promptTemplateId(),
                response.promptContent(),
                response.prompt(),
                response.openAiRequestBody(),
                response.openAiModel(),
                response.schemaJson(),
                response.promptMarkdownContent(),
                response.status(),
                response.openAiJobId(),
                response.modelResponse(),
                response.provisionalHtml(),
                response.errorMessage(),
                response.errorDetail(),
                response.inputTokens(),
                response.outputTokens(),
                response.costUsd());
    }
}

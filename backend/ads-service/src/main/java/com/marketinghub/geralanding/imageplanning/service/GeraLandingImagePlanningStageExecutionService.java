package com.marketinghub.geralanding.imageplanning.service;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsável por adaptar consultas de execução para o contrato da etapa imageplanning. */
@Service
public class GeraLandingImagePlanningStageExecutionService {

    private final GeraLandingStageExecutionService delegate;

    public GeraLandingImagePlanningStageExecutionService(GeraLandingStageExecutionService delegate) {
        this.delegate = delegate;
    }


    /** Registra a execução inicial da etapa convertendo para o DTO local de início. */
    public GeraLandingImagePlanningStartResponse registerInitialExecution(Long experimentId, String stageCode) {
        var response = delegate.registerInitialExecution(experimentId, stageCode);
        return new GeraLandingImagePlanningStartResponse(response.idJob(), response.status());
    }

    /** Lista execuções da etapa convertendo para o DTO local da etapa. */
    public List<GeraLandingImagePlanningExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, String stageCode, boolean includeCompleted) {
        return delegate.listExperimentStageExecutions(experimentId, stageCode, includeCompleted).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Retorna o detalhe da execução convertido para o DTO local da etapa. */
    public GeraLandingImagePlanningStageExecutionDetailResponse getStageExecutionDetail(Long experimentId, String idJob) {
        return toDetailResponse(delegate.getStageExecutionDetail(experimentId, idJob));
    }

    /** Converte o resumo transversal para o resumo local da etapa. */
    private GeraLandingImagePlanningExecutionSummaryResponse toSummaryResponse(GeraLandingExecutionSummaryResponse response) {
        return new GeraLandingImagePlanningExecutionSummaryResponse(
                response.idJob(),
                response.status(),
                response.executionRequestedAt(),
                response.costUsd());
    }

    /** Converte o detalhe transversal para o detalhe local da etapa. */
    private GeraLandingImagePlanningStageExecutionDetailResponse toDetailResponse(GeraLandingStageExecutionDetailResponse response) {
        return new GeraLandingImagePlanningStageExecutionDetailResponse(
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

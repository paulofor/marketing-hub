package com.marketinghub.worker.openai.core.imageplanning;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: registrar logs operacionais de sucesso e falha da etapa image planning. */
public class ImagePlanningResponseHandler implements StageResponseHandler<ImagePlanningInput, ImagePlanningOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImagePlanningResponseHandler.class);

    /** Registra o sucesso da execução com identificadores e métricas de uso da OpenAI. */
    @Override
    public void handleSuccess(
            StageExecution<ImagePlanningInput> execution,
            OpenAiResult<ImagePlanningOutput> result
    ) {
        log.info(
                "ImagePlanning execution completed. idJob={}, experimentId={}, inputTokens={}, outputTokens={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd()
        );
    }

    /** Registra a falha da execução preservando a stack trace completa no log. */
    @Override
    public void handleFailure(StageExecution<ImagePlanningInput> execution, Throwable error) {
        log.error(
                "ImagePlanning execution failed. idJob={}, experimentId={}",
                execution.idJob(),
                execution.aggregateId(),
                error
        );
    }
}

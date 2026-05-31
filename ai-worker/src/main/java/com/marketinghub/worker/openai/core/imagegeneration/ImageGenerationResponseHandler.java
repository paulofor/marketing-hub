package com.marketinghub.worker.openai.core.imagegeneration;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: registrar o resultado operacional das execuções da etapa imagegeneration. */
public class ImageGenerationResponseHandler implements StageResponseHandler<ImageGenerationInput, ImageGenerationOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationResponseHandler.class);

    /** Registra os dados resumidos de sucesso após a resposta validada da etapa imagegeneration. */
    @Override
    public void handleSuccess(StageExecution<ImageGenerationInput> execution, OpenAiResult<ImageGenerationOutput> result) {
        log.info(
                "ImageGeneration execution completed. idJob={}, experimentId={}, openAiJobId={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                result.openAiJobId(),
                result.costUsd()
        );
    }

    /** Registra a falha completa da etapa imagegeneration preservando stack trace para diagnóstico. */
    @Override
    public void handleFailure(StageExecution<ImageGenerationInput> execution, Throwable error) {
        log.error(
                "ImageGeneration execution failed. idJob={}, experimentId={}",
                execution.idJob(),
                execution.aggregateId(),
                error
        );
    }
}

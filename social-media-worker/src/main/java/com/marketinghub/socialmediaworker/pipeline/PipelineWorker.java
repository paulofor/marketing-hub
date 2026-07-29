package com.marketinghub.socialmediaworker.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executa uma etapa concreta sem conhecer detalhes da plataforma social.
 */
public class PipelineWorker<I, O> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineWorker.class);

    private final StageProcessor<I, O> processor;

    /**
     * Recebe o processor plugavel que executa a etapa concreta.
     */
    public PipelineWorker(StageProcessor<I, O> processor) {
        this.processor = processor;
    }

    /**
     * Executa o processor e converte excecoes em falha auditavel.
     */
    public StageResult<O> run(StageContext context, I input) {
        try {
            return StageResult.success(processor.process(context, input));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn(
                    "Falha de validacao em etapa de pipeline: stageCode={}, executionId={}, jobId={}, erro={}",
                    context.stageCode(),
                    context.executionId(),
                    context.jobId(),
                    ex.getMessage(),
                    ex);
            return StageResult.failure("VALIDATION_ERROR", ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Falha tecnica em etapa de pipeline: stageCode={}, executionId={}, jobId={}, erro={}",
                    context.stageCode(),
                    context.executionId(),
                    context.jobId(),
                    ex.getMessage(),
                    ex);
            return StageResult.failure("TECHNICAL_ERROR", ex.getMessage());
        }
    }
}

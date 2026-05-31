package com.marketinghub.worker.openai.core.presetdesign;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: registrar os resultados operacionais da etapa presetdesign. */
public class PresetDesignResponseHandler implements StageResponseHandler<PresetDesignInput, PresetDesignOutput> {

    private static final Logger log = LoggerFactory.getLogger(PresetDesignResponseHandler.class);

    /** Registra em log os metadados de uma execução presetdesign concluída com sucesso. */
    @Override
    public void handleSuccess(
            StageExecution<PresetDesignInput> execution,
            OpenAiResult<PresetDesignOutput> result
    ) {
        log.info(
                "PresetDesign execution completed. idJob={}, experimentId={}, inputTokens={}, outputTokens={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd()
        );
    }

    /** Registra em log a falha da execução presetdesign preservando a stack trace. */
    @Override
    public void handleFailure(StageExecution<PresetDesignInput> execution, Throwable error) {
        log.error(
                "PresetDesign execution failed. idJob={}, experimentId={}",
                execution.idJob(),
                execution.aggregateId(),
                error
        );
    }
}

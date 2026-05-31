package com.marketinghub.worker.openai.core.copy;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: registrar o resultado operacional das execuções da etapa copy. */
public class CopyResponseHandler implements StageResponseHandler<CopyInput, CopyOutput> {

    private static final Logger log = LoggerFactory.getLogger(CopyResponseHandler.class);

    /** Registra os dados resumidos de sucesso após a resposta validada da etapa copy. */
    @Override
    public void handleSuccess(
            StageExecution<CopyInput> execution,
            OpenAiResult<CopyOutput> result
    ) {
        log.info(
                "Copy execution completed. idJob={}, experimentId={}, inputTokens={}, outputTokens={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd()
        );
    }

    /** Registra a falha completa da etapa copy preservando stack trace para diagnóstico. */
    @Override
    public void handleFailure(StageExecution<CopyInput> execution, Throwable error) {
        log.error(
                "Copy execution failed. idJob={}, experimentId={}",
                execution.idJob(),
                execution.aggregateId(),
                error
        );
    }
}

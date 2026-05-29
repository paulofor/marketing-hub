package com.marketinghub.worker.openai.core.wireframe;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WireframeResponseHandler implements StageResponseHandler<WireframeInput, WireframeOutput> {

    private static final Logger log = LoggerFactory.getLogger(WireframeResponseHandler.class);

    @Override
    public void handleSuccess(
            StageExecution<WireframeInput> execution,
            OpenAiResult<WireframeOutput> result
    ) {
        log.info(
                "Wireframe execution completed. idJob={}, experimentId={}, inputTokens={}, outputTokens={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd()
        );
    }

    @Override
    public void handleFailure(StageExecution<WireframeInput> execution, Throwable error) {
        log.error(
                "Wireframe execution failed. idJob={}, experimentId={}",
                execution.idJob(),
                execution.aggregateId(),
                error
        );
    }
}

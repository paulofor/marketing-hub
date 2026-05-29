package com.marketinghub.worker.openai.core;

import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.ProcessingSummary;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.model.StageWorkerResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.openai.core.port.StageBackendPort;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StageWorker<I, O> {

    private final StageBackendPort<I, O> backendPort;
    private final StagePromptBuilder<I> promptBuilder;
    private final OpenAiClientPort openAiClient;
    private final StageResponseValidator<O> responseValidator;
    private final StageResponseHandler<I, O> responseHandler;

    public StageWorker(
            StageBackendPort<I, O> backendPort,
            StagePromptBuilder<I> promptBuilder,
            OpenAiClientPort openAiClient,
            StageResponseValidator<O> responseValidator,
            StageResponseHandler<I, O> responseHandler
    ) {
        this.backendPort = Objects.requireNonNull(backendPort, "backendPort must not be null");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder must not be null");
        this.openAiClient = Objects.requireNonNull(openAiClient, "openAiClient must not be null");
        this.responseValidator = Objects.requireNonNull(responseValidator, "responseValidator must not be null");
        this.responseHandler = Objects.requireNonNull(responseHandler, "responseHandler must not be null");
    }

    public ProcessingSummary processPending(int limit) {
        List<StageExecution<I>> pending = backendPort.listPending(limit);
        List<StageWorkerResult> results = new ArrayList<>();

        for (StageExecution<I> execution : pending) {
            results.add(process(execution));
        }

        return ProcessingSummary.from(results);
    }

    public StageWorkerResult process(StageExecution<I> execution) {
        Objects.requireNonNull(execution, "execution must not be null");

        try {
            OpenAiRequest request = promptBuilder.build(execution);

            OpenAiDispatch dispatch = openAiClient.dispatch(request);
            backendPort.markDispatched(execution, dispatch);

            OpenAiResult<String> rawResult = openAiClient.awaitResult(dispatch);

            O parsedResponse = responseValidator.validateAndParse(rawResult.modelResponse());
            OpenAiResult<O> typedResult = rawResult.withParsedResponse(parsedResponse);

            responseHandler.handleSuccess(execution, typedResult);
            backendPort.markCompleted(execution, typedResult);

            return StageWorkerResult.success(execution.idJob());
        } catch (Exception error) {
            responseHandler.handleFailure(execution, error);
            backendPort.markFailed(execution, error);
            return StageWorkerResult.failure(execution.idJob(), error);
        }
    }
}

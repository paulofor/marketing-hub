package com.marketinghub.pipelines.nichocnae.v3.execution;

import com.marketinghub.pipelines.nichocnae.v3.core.PipelineWorker;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Serviço operacional que executa pendências NichoCNAE v3 no módulo OPRM. */
@Service
public class NichoCnaeV3PendingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(NichoCnaeV3PendingExecutionService.class);
    private static final String SOURCE_SEARCHER_STAGE = "source-searcher";
    private final NichoCnaeV3BackendClient backendClient;
    private final NichoCnaeV3StageDefinitions stageDefinitions;
    private final long sourceSearcherMaxJobDurationMs;

    /** Inicializa o executor com cliente backend, catálogo de etapas v3 e limite operacional do source-searcher. */
    public NichoCnaeV3PendingExecutionService(NichoCnaeV3BackendClient backendClient, NichoCnaeV3StageDefinitions stageDefinitions,
            @Value("${oprm.pipelines.nichocnae.v3.source-searcher.max-job-duration-ms:120000}") long sourceSearcherMaxJobDurationMs) {
        this.backendClient = backendClient;
        this.stageDefinitions = stageDefinitions;
        this.sourceSearcherMaxJobDurationMs = sourceSearcherMaxJobDurationMs;
    }

    /** Processa todas as pendências v3 disponíveis. */
    public int processAllPending() {
        int processed = 0;
        for (NichoCnaeV3StageDefinition stage : stageDefinitions.all()) {
            processed += processStage(stage);
        }
        return processed;
    }

    /** Processa as pendências de uma etapa específica. */
    private int processStage(NichoCnaeV3StageDefinition stage) {
        List<NichoCnaeV3PendingExecution> pendingExecutions = backendClient.listPending(stage);
        int processed = 0;
        for (NichoCnaeV3PendingExecution pending : pendingExecutions) {
            processOne(stage, pending);
            processed++;
        }
        return processed;
    }

    /** Executa uma pendência e reporta o resultado para o backend decidir o avanço. */
    private void processOne(NichoCnaeV3StageDefinition stage, NichoCnaeV3PendingExecution pending) {
        try {
            Map<String, Object> input = backendClient.parseInput(pending.inputPayload());
            input.putIfAbsent("cnaeCode", pending.cnaeCode());
            StageResult result = executeStage(stage, pending, input);
            String outputPayload = backendClient.toJson(result.output());
            Map<String, Object> completion = new LinkedHashMap<>();
            completion.put("outputPayload", outputPayload);
            completion.put("nextStageCode", result.output().get("nextStageCode"));
            backendClient.complete(stage, pending, completion);
        } catch (RuntimeException ex) {
            log.error("Erro ao processar pendência NichoCNAE v3 (stage={}, stageExecutionId={}, jobId={})", stage.stageCode(), pending.stageExecutionId(), pending.jobId(), ex);
            backendClient.fail(stage, pending, ex);
        }
    }

    /** Executa a etapa aplicando timeout operacional específico ao source-searcher. */
    private StageResult executeStage(NichoCnaeV3StageDefinition stage, NichoCnaeV3PendingExecution pending, Map<String, Object> input) {
        StageContext context = new StageContext(pending.jobId(), pending.stageExecutionId(), input);
        if (!SOURCE_SEARCHER_STAGE.equals(stage.stageCode()) || sourceSearcherMaxJobDurationMs <= 0) {
            return new PipelineWorker(stage.processor()).run(context);
        }
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name("nichocnae-v3-source-searcher-timeout-", 0)
                .factory());
        Future<StageResult> future = executor.submit(() -> new PipelineWorker(stage.processor()).run(context));
        try {
            return future.get(sourceSearcherMaxJobDurationMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new SourceSearcherTimeoutException("source-searcher excedeu " + sourceSearcherMaxJobDurationMs
                    + "ms e foi cancelado para liberar a fila do NichoCNAE v3.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SourceSearcherTimeoutException("source-searcher interrompido durante execução do job.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("source-searcher falhou com exceção não operacional.", cause);
        } finally {
            executor.shutdownNow();
        }
    }

    /** Sinaliza cancelamento operacional do source-searcher por excesso de duração. */
    private static final class SourceSearcherTimeoutException extends RuntimeException {
        /** Cria exceção de timeout preservando a causa técnica original. */
        private SourceSearcherTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

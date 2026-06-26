package com.marketinghub.pipelines.nichocnae.v3.execution;

import com.marketinghub.pipelines.nichocnae.v3.core.PipelineWorker;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Serviço operacional que executa pendências NichoCNAE v3 no módulo OPRM. */
@Service
public class NichoCnaeV3PendingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(NichoCnaeV3PendingExecutionService.class);
    private final NichoCnaeV3BackendClient backendClient;
    private final NichoCnaeV3StageDefinitions stageDefinitions;

    /** Inicializa o executor com cliente backend e catálogo de etapas v3. */
    public NichoCnaeV3PendingExecutionService(NichoCnaeV3BackendClient backendClient, NichoCnaeV3StageDefinitions stageDefinitions) {
        this.backendClient = backendClient;
        this.stageDefinitions = stageDefinitions;
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
            StageResult result = new PipelineWorker(stage.processor()).run(new StageContext(pending.jobId(), pending.stageExecutionId(), input));
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
}
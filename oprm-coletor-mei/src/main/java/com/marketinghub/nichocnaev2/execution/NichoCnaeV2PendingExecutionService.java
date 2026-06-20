package com.marketinghub.nichocnaev2.execution;

import com.marketinghub.nichocnaev2.pipeline.PipelineWorker;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Executa pendências NichoCNAE v2 publicadas pelo backend para todas as etapas cadastradas. */
@Service
public class NichoCnaeV2PendingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(NichoCnaeV2PendingExecutionService.class);
    private static final int MAX_TECHNICAL_RETRIES_PER_ATTEMPT = 3;

    private final NichoCnaeV2BackendClient backendClient;
    private final NichoCnaeV2StageDefinitions stageDefinitions;

    /** Inicializa o serviço com o cliente do backend e o catálogo de etapas v2. */
    public NichoCnaeV2PendingExecutionService(
            NichoCnaeV2BackendClient backendClient, NichoCnaeV2StageDefinitions stageDefinitions) {
        this.backendClient = backendClient;
        this.stageDefinitions = stageDefinitions;
    }

    /** Consulta e processa pendências de todas as etapas registradas no pipeline NichoCNAE v2. */
    public int processAllPending() {
        int processed = 0;
        for (NichoCnaeV2StageDefinition stage : stageDefinitions.all()) {
            processed += processStagePending(stage);
        }
        return processed;
    }

    /** Consulta o pending de uma etapa, executa cada item retornado e reporta conclusão ou falha. */
    private int processStagePending(NichoCnaeV2StageDefinition stage) {
        List<NichoCnaeV2PendingExecution> pendingExecutions = backendClient.listPending(stage);
        if (pendingExecutions.isEmpty()) {
            log.info("Nenhuma pendência NichoCNAE v2 encontrada (stage={})", stage.stageCode());
            return 0;
        }
        int processed = 0;
        for (NichoCnaeV2PendingExecution pending : pendingExecutions) {
            processOne(stage, pending);
            processed++;
        }
        return processed;
    }

    /** Executa um item pendente preservando callback de falha com stack trace completo em log. */
    private void processOne(NichoCnaeV2StageDefinition stage, NichoCnaeV2PendingExecution pending) {
        try {
            log.info(
                    "Processando pendência NichoCNAE v2 (stage={}, stageExecutionId={}, jobId={}, cnaeCode={}, sourceNicheId={})",
                    stage.stageCode(),
                    pending.stageExecutionId(),
                    pending.jobId(),
                    pending.cnaeCode(),
                    pending.sourceNicheId());
            Map<String, Object> input = inputFor(pending);
            StageResult result = new PipelineWorker(stage.processor())
                    .run(new StageContext(pending.jobId(), pending.stageExecutionId(), input));
            String outputPayload = backendClient.toJson(result.output());
            Map<String, Object> completion = completionRequest(stage.stageCode(), result, outputPayload);
            backendClient.complete(stage, pending, completion);
            createNextStageIfNeeded(stage, pending, result, outputPayload);
            log.info(
                    "Pendência NichoCNAE v2 concluída (stage={}, stageExecutionId={}, jobId={}, status={}, nextStageCode={})",
                    stage.stageCode(),
                    pending.stageExecutionId(),
                    pending.jobId(),
                    result.status(),
                    result.output().get("nextStageCode"));
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao processar pendência NichoCNAE v2 (stage={}, stageExecutionId={}, jobId={}, cnaeCode={}, sourceNicheId={})",
                    stage.stageCode(),
                    pending.stageExecutionId(),
                    pending.jobId(),
                    pending.cnaeCode(),
                    pending.sourceNicheId(),
                    ex);
            registerFailure(stage, pending, ex);
        }
    }

    /** Classifica falhas no executor para evitar retry técnico infinito em erro de contrato ou limite excedido. */
    private void registerFailure(NichoCnaeV2StageDefinition stage, NichoCnaeV2PendingExecution pending, RuntimeException ex) {
        String failureType = "INFRASTRUCTURE";
        String reasonCode = "SCHEDULER_PROCESSING_ERROR";
        if (ex instanceof IllegalArgumentException) {
            failureType = "VALIDATION";
            reasonCode = "INVALID_STAGE_INPUT_CONTRACT";
        } else if (technicalRetryNumber(pending) >= MAX_TECHNICAL_RETRIES_PER_ATTEMPT) {
            failureType = "VALIDATION";
            reasonCode = "TECHNICAL_RETRY_LIMIT_EXCEEDED";
        }
        backendClient.fail(stage, pending, ex, failureType, reasonCode);
    }

    /** Normaliza o contador de retry técnico ausente para zero antes de comparar com o limite operacional. */
    private int technicalRetryNumber(NichoCnaeV2PendingExecution pending) {
        return pending.technicalRetryNumber() == null ? 0 : pending.technicalRetryNumber();
    }

    /** Monta o input do processor com JSON estruturado e metadados do pending sem JSON dentro de JSON. */
    private Map<String, Object> inputFor(NichoCnaeV2PendingExecution pending) {
        Map<String, Object> input = new LinkedHashMap<>(backendClient.parseInput(pending.inputPayload()));
        input.putIfAbsent("stageExecutionId", pending.stageExecutionId());
        input.putIfAbsent("jobId", pending.jobId());
        input.putIfAbsent("cnaeCode", pending.cnaeCode());
        input.putIfAbsent("cnaeDescription", pending.cnaeDescription());
        input.putIfAbsent("sourceNicheId", pending.sourceNicheId());
        input.putIfAbsent("attemptNumber", pending.attemptNumber());
        input.putIfAbsent("technicalRetryNumber", pending.technicalRetryNumber());
        input.putIfAbsent("knowledgeVersion", pending.knowledgeVersion());
        input.putIfAbsent("materializationEnabled", pending.materializationEnabled());
        return input;
    }

    /** Monta o corpo de complete compatível com cada contrato de etapa do backend. */
    private Map<String, Object> completionRequest(String stageCode, StageResult result, String outputPayload) {
        Map<String, Object> output = result.output();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("outputPayload", outputPayload);
        request.put("nextStageCode", output.get("nextStageCode"));
        switch (stageCode) {
            case "candidate-generator" -> {
                request.put("qualityStatus", result.status());
                request.put("requestedAction", "CONTINUE_PIPELINE");
                request.putIfAbsent("nextStageCode", "source-safety-filter");
            }
            case "source-safety-filter" -> {
                request.put("safetyDecision", output.getOrDefault("safetyDecision", result.status()));
                request.put("allowedUrlCount", output.get("allowedUrlCount"));
                request.put("rejectedUrlCount", output.get("rejectedUrlCount"));
                request.putIfAbsent("nextStageCode", "adaptive-query-planner");
            }
            case "adaptive-query-planner" -> {
                request.put("planDecision", output.getOrDefault("planDecision", result.status()));
                request.put("plannedQueryCount", output.get("plannedQueryCount"));
                request.put("reusedQueryCount", output.get("reusedQueryCount"));
                request.put("skippedQueryCount", output.get("skippedQueryCount"));
            }
            case "candidate-tournament" -> {
                request.put("tournamentDecision", output.getOrDefault("tournamentDecision", result.status()));
                request.put("candidateCount", output.get("candidateCount"));
                request.put("finalistCount", output.get("finalistCount"));
            }
            case "source-fetcher-reranker" -> {
                request.put("sourceFetchDecision", output.getOrDefault("sourceFetchDecision", result.status()));
                request.put("fetchedSnapshotCount", output.get("fetchedSnapshotCount"));
                request.put("selectedSourceCount", output.get("selectedSourceCount"));
                request.put("rejectedSourceCount", output.get("rejectedSourceCount"));
            }
            case "knowledge-accumulator" -> {
                request.put("knowledgeVersion", output.get("knowledgeVersion"));
                request.put("validatedFactCount", output.get("validatedFactCount"));
                request.put("acceptedSourceCount", output.get("acceptedSourceCount"));
                request.put("rejectedSourceCount", output.get("rejectedSourceCount"));
            }
            case "commercial-evidence-gate" -> {
                request.put("evidenceLevel", output.get("evidenceLevel"));
                request.put("confidence", output.get("confidence"));
                request.put("automaticMaterializationAllowed", output.get("automaticMaterializationAllowed"));
                request.put("humanReviewRequired", output.get("humanReviewRequired"));
                request.put("informationGain", output.get("informationGain"));
                request.put("gateDecision", output.getOrDefault("gateDecision", result.status()));
            }
            case "reprocess-controller" -> {
                request.put("executionMode", output.get("executionMode"));
                request.put("rewindToStage", output.get("rewindToStage"));
                request.put("knowledgeVersionTo", output.get("knowledgeVersionTo"));
            }
            case "enriched-niche-materializer" -> {
                request.put("materializationDecision", output.getOrDefault("materializationDecision", result.status()));
                request.put("validationLevel", output.get("validationLevel"));
                request.put("confidence", output.get("confidence"));
                request.put("materializedNicheId", output.get("materializedNicheId"));
            }
            default -> throw new IllegalArgumentException("Etapa NichoCNAE v2 sem contrato de conclusão: " + stageCode);
        }
        return request;
    }

    /** Cria a próxima pendência quando existe uma etapa v2 correspondente no catálogo local. */
    private void createNextStageIfNeeded(
            NichoCnaeV2StageDefinition currentStage,
            NichoCnaeV2PendingExecution pending,
            StageResult result,
            String outputPayload) {
        String nextStageCode = String.valueOf(result.output().getOrDefault("nextStageCode", "")).trim();
        if (nextStageCode.isBlank() && "candidate-generator".equals(currentStage.stageCode())) {
            nextStageCode = "source-safety-filter";
        }
        if (nextStageCode.isBlank() && "source-safety-filter".equals(currentStage.stageCode())) {
            nextStageCode = "adaptive-query-planner";
        }
        if (nextStageCode.isBlank()) {
            return;
        }
        String resolvedNextStageCode = nextStageCode;
        Optional<NichoCnaeV2StageDefinition> nextStage = stageDefinitions.all().stream()
                .filter(stage -> stage.stageCode().equals(resolvedNextStageCode))
                .findFirst();
        if (nextStage.isEmpty()) {
            log.info(
                    "Próxima etapa NichoCNAE v2 não possui executor local cadastrado; somente conclusão foi registrada (currentStage={}, nextStageCode={}, stageExecutionId={})",
                    currentStage.stageCode(),
                    nextStageCode,
                    pending.stageExecutionId());
            return;
        }
        backendClient.createNextStage(
                nextStage.get(),
                pending,
                outputPayload,
                integer(result.output().get("attemptNumber")),
                integer(result.output().get("knowledgeVersionTo")));
    }

    /** Converte metadado opcional da etapa para inteiro sem quebrar avanço quando ausente. */
    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null || String.valueOf(value).isBlank() ? null : Integer.valueOf(String.valueOf(value));
    }
}

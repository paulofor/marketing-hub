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
    private static final int MAX_STAGE_VISITS_PER_JOB = 3;
    private static final int MAX_NO_INFORMATION_GAIN_STREAK = 3;

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
            if (shouldStopBecauseOfResearchLoop(stage, pending, input, result)) {
                registerControlledResearchLoop(stage, pending, input, result);
                return;
            }
            String outputPayload = backendClient.toJson(result.output());
            Map<String, Object> completion = completionRequest(stage.stageCode(), result, outputPayload);
            backendClient.complete(stage, pending, completion);
            createNextStageIfNeeded(stage, pending, input, result);
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

    /** Registra falha controlada quando o job repete pesquisa sem ganho novo de evidência útil. */
    private void registerControlledResearchLoop(
            NichoCnaeV2StageDefinition stage,
            NichoCnaeV2PendingExecution pending,
            Map<String, Object> input,
            StageResult result) {
        Map<String, Object> diagnosticPayload = nextStageInputPayload(input, result, text(result.output().get("nextStageCode")));
        String message = "Job NichoCNAE v2 encerrado por ciclo de pesquisa sem ganho novo: stage="
                + stage.stageCode()
                + "; jobId="
                + pending.jobId()
                + "; cnaeCode="
                + pending.cnaeCode()
                + "; stageVisitCounts="
                + diagnosticPayload.get("stageVisitCounts")
                + "; noInformationGainStreak="
                + diagnosticPayload.get("noInformationGainStreak")
                + "; nextRecommendedAction=trocar recorte de subnicho ou iniciar nova pesquisa manual com hipótese de público mais específica";
        log.warn(message);
        backendClient.fail(
                stage,
                pending,
                new ControlledResearchLoopException(message),
                "MARKET_EVIDENCE",
                "RESEARCH_LOOP_WITHOUT_INFORMATION_GAIN");
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
            Map<String, Object> input,
            StageResult result) {
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
        String nextStageInputPayload = backendClient.toJson(nextStageInputPayload(input, result, nextStageCode));
        backendClient.createNextStage(
                nextStage.get(),
                pending,
                nextStageInputPayload,
                integer(result.output().get("attemptNumber")),
                integer(result.output().get("knowledgeVersionTo")));
    }

    /** Preserva o contexto funcional acumulado e sobrepõe apenas a saída nova da etapa concluída. */
    private Map<String, Object> nextStageInputPayload(Map<String, Object> input, StageResult result, String nextStageCode) {
        Map<String, Object> payload = new LinkedHashMap<>(input);
        payload.remove("nextStageCode");
        payload.putAll(result.output());
        payload.put("nextStageCode", nextStageCode);
        payload.put("stageVisitCounts", updatedStageVisitCounts(input, result));
        payload.put("noInformationGainStreak", updatedNoInformationGainStreak(input, result));
        return payload;
    }

    /** Decide se a próxima pendência criaria um ciclo operacional sem aprendizado suficiente para continuar. */
    private boolean shouldStopBecauseOfResearchLoop(
            NichoCnaeV2StageDefinition stage,
            NichoCnaeV2PendingExecution pending,
            Map<String, Object> input,
            StageResult result) {
        String nextStageCode = text(result.output().get("nextStageCode"));
        if (nextStageCode.isBlank()) {
            return false;
        }
        Map<String, Integer> visitCounts = updatedStageVisitCounts(input, result);
        int currentStageVisits = visitCounts.getOrDefault(stage.stageCode(), 0);
        int noInformationGainStreak = updatedNoInformationGainStreak(input, result);
        boolean stageRepeatedTooMuch = currentStageVisits >= MAX_STAGE_VISITS_PER_JOB && noInformationGain(result);
        boolean noGainTooLong = noInformationGainStreak >= MAX_NO_INFORMATION_GAIN_STREAK;
        boolean loopingBetweenResearchStages = isResearchLoopStage(stage.stageCode()) && isResearchLoopStage(nextStageCode);
        if (loopingBetweenResearchStages && (stageRepeatedTooMuch || noGainTooLong)) {
            log.warn(
                    "Ciclo de pesquisa NichoCNAE v2 detectado antes de criar próxima etapa (stage={}, nextStage={}, jobId={}, cnaeCode={}, visits={}, noGainStreak={})",
                    stage.stageCode(),
                    nextStageCode,
                    pending.jobId(),
                    pending.cnaeCode(),
                    currentStageVisits,
                    noInformationGainStreak);
            return true;
        }
        return false;
    }

    /** Atualiza o contador de visitas por etapa preservado no payload funcional do job. */
    private Map<String, Integer> updatedStageVisitCounts(Map<String, Object> input, StageResult result) {
        Map<String, Integer> visitCounts = intMap(input.get("stageVisitCounts"));
        String currentStage = text(result.output().get("stage"));
        if (!currentStage.isBlank()) {
            visitCounts.put(currentStage, visitCounts.getOrDefault(currentStage, 0) + 1);
        }
        return visitCounts;
    }

    /** Atualiza a sequência de etapas que não acrescentaram evidência, fonte ou query nova ao job. */
    private int updatedNoInformationGainStreak(Map<String, Object> input, StageResult result) {
        int current = integer(input.get("noInformationGainStreak")) == null ? 0 : integer(input.get("noInformationGainStreak"));
        return noInformationGain(result) ? current + 1 : 0;
    }

    /** Identifica decisões que representam ausência objetiva de avanço de pesquisa. */
    private boolean noInformationGain(StageResult result) {
        Map<String, Object> output = result.output();
        String status = text(result.status()).toUpperCase();
        String decision = text(output.getOrDefault(
                        "planDecision",
                        output.getOrDefault("sourceFetchDecision", output.getOrDefault("tournamentDecision", ""))))
                .toUpperCase();
        Integer plannedQueryCount = integer(output.get("plannedQueryCount"));
        Integer selectedSourceCount = integer(output.get("selectedSourceCount"));
        Integer finalistCount = integer(output.get("finalistCount"));
        return status.contains("NO_RESEARCH_GAIN")
                || status.contains("NO_FETCHABLE_DIRECT_SOURCE")
                || status.contains("NO_VIABLE_SUBNICHE")
                || decision.contains("NO_RESEARCH_GAIN")
                || decision.contains("NO_FETCHABLE_DIRECT_SOURCE")
                || decision.contains("NO_VIABLE_SUBNICHE")
                || Integer.valueOf(0).equals(plannedQueryCount)
                || Integer.valueOf(0).equals(selectedSourceCount)
                || Integer.valueOf(0).equals(finalistCount);
    }

    /** Limita a trava aos estágios que historicamente podem formar circuito de replanejamento de pesquisa. */
    private boolean isResearchLoopStage(String stageCode) {
        return "adaptive-query-planner".equals(stageCode)
                || "candidate-tournament".equals(stageCode)
                || "source-fetcher-reranker".equals(stageCode)
                || "reprocess-controller".equals(stageCode);
    }

    /** Converte mapa livre do payload em contadores inteiros usados pela trava anti-ciclo. */
    private Map<String, Integer> intMap(Object value) {
        Map<String, Integer> converted = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, val) -> {
                Integer parsed = integer(val);
                if (parsed != null) {
                    converted.put(String.valueOf(key), parsed);
                }
            });
        }
        return converted;
    }

    /** Converte metadado opcional da etapa para inteiro sem quebrar avanço quando ausente. */
    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null || String.valueOf(value).isBlank() ? null : Integer.valueOf(String.valueOf(value));
    }

    /** Extrai texto normalizado de campos livres do payload sem quebrar valores ausentes. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Exceção operacional usada para registrar falha controlada por ciclo de pesquisa sem ganho novo. */
    private static final class ControlledResearchLoopException extends RuntimeException {
        /** Inicializa a exceção com mensagem de negócio persistível no backend. */
        private ControlledResearchLoopException(String message) {
            super(message);
        }
    }
}

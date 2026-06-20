package com.marketinghub.nichocnaev2.pipeline.reprocesscontroller;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Decide retry técnico ou reprocessamento cognitivo do NichoCNAE v2 no executor externo. */
public final class ReprocessControllerProcessor implements StageProcessor {
    private static final List<String> INFRASTRUCTURE_REASONS = List.of("TIMEOUT", "SSL", "BROKEN_PIPE", "HTTP_429", "HTTP_502", "HTTP_503", "HTTP_504", "CONNECTION");

    /** Calcula o menor retorno necessário preservando conhecimento válido e sem reiniciar o ciclo inteiro. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> input = context.input();
        String failureType = text(input.get("failureType")).toUpperCase(Locale.ROOT);
        String reasonCode = text(input.get("reasonCode")).toUpperCase(Locale.ROOT);
        String gateDecision = text(input.getOrDefault("gateDecision", input.get("tournamentDecision"))).toUpperCase(Locale.ROOT);
        int attemptNumber = integer(input.get("attemptNumber"), 1);
        int technicalRetryNumber = integer(input.get("technicalRetryNumber"), 0);
        int knowledgeVersion = integer(input.get("knowledgeVersion"), 1);
        int maxCognitiveAttempts = integer(input.get("maxCognitiveAttempts"), 3);
        double informationGain = decimal(input.get("informationGain"), 0.0);
        boolean technical = "INFRASTRUCTURE".equals(failureType) || INFRASTRUCTURE_REASONS.stream().anyMatch(reasonCode::contains);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("researchCycleId", input.get("researchCycleId"));
        plan.put("candidateId", input.get("candidateId"));
        plan.put("candidateVersion", integer(input.get("candidateVersion"), 1));
        plan.put("reasonCode", reasonCode.isBlank() ? gateDecision : reasonCode);
        plan.put("preservedArtifacts", input.getOrDefault("preservedArtifacts", List.of("VALIDATED_FACTS", "ACCEPTED_SOURCES", "QUERY_MEMORY")));
        plan.put("invalidatedArtifacts", invalidatedArtifacts(reasonCode, gateDecision));

        if (technical) {
            plan.put("executionMode", "TECHNICAL_RETRY");
            plan.put("rewindToStage", text(input.getOrDefault("failedStageCode", input.get("stageCode"))));
            plan.put("attemptNumber", attemptNumber);
            plan.put("technicalRetryNumber", technicalRetryNumber + 1);
            plan.put("knowledgeVersion", knowledgeVersion);
            plan.put("nextStageCode", text(plan.get("rewindToStage")));
            return result("TECHNICAL_RETRY_PLANNED", plan, "Retry técnico preservando a mesma versão de conhecimento.");
        }

        if (attemptNumber >= maxCognitiveAttempts && informationGain < 0.10) {
            plan.put("executionMode", "STOP_NO_INFORMATION_GAIN");
            plan.put("rewindToStage", "end");
            plan.put("attemptNumber", attemptNumber);
            plan.put("technicalRetryNumber", technicalRetryNumber);
            plan.put("knowledgeVersion", knowledgeVersion);
            plan.put("nextStageCode", "end");
            return result("NO_INFORMATION_GAIN", plan, "Encerramento seguro por falta de ganho informacional.");
        }

        plan.put("executionMode", "COGNITIVE_REPROCESS");
        plan.put("rewindToStage", rewindToStage(reasonCode, gateDecision));
        plan.put("newEvidenceGaps", input.getOrDefault("evidenceGaps", input.getOrDefault("missingEvidence", List.of())));
        plan.put("attemptNumber", attemptNumber + 1);
        plan.put("technicalRetryNumber", 0);
        plan.put("knowledgeVersionFrom", knowledgeVersion);
        plan.put("knowledgeVersionTo", knowledgeVersion + 1);
        plan.put("nextStageCode", plan.get("rewindToStage"));
        return result("COGNITIVE_REPROCESS_PLANNED", plan, "Reprocessamento cognitivo com menor rewind necessário e conhecimento preservado.");
    }

    /** Monta o StageResult persistível do plano de reprocessamento. */
    private StageResult result(String status, Map<String, Object> plan, String description) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "reprocess-controller");
        output.put("reprocessPlan", plan);
        output.put("executionMode", plan.get("executionMode"));
        output.put("rewindToStage", plan.get("rewindToStage"));
        output.put("attemptNumber", plan.get("attemptNumber"));
        output.put("technicalRetryNumber", plan.get("technicalRetryNumber"));
        output.put("knowledgeVersionTo", plan.getOrDefault("knowledgeVersionTo", plan.get("knowledgeVersion")));
        output.put("nextStageCode", plan.get("nextStageCode"));
        return new StageResult(status, output, List.of(new StageArtifact("REPROCESS_PLAN", "inline://reprocess-controller/plan", description)));
    }

    /** Escolhe a menor etapa de retorno conforme o diagnóstico do gate. */
    private String rewindToStage(String reasonCode, String gateDecision) {
        if (reasonCode.contains("SOURCE") || reasonCode.contains("DOMAIN")) return "source-fetcher-reranker";
        if (reasonCode.contains("CLAIM") || reasonCode.contains("CONTRADICT")) return "signal-extractor";
        if (reasonCode.contains("CANDIDATE") || "NO_VIABLE_SUBNICHE".equals(gateDecision)) return "candidate-tournament";
        return "adaptive-query-planner";
    }

    /** Declara quais descendentes devem ser recalculados sem apagar evidência auditável. */
    private List<String> invalidatedArtifacts(String reasonCode, String gateDecision) {
        if (reasonCode.contains("SOURCE") || reasonCode.contains("DOMAIN")) return List.of("SOURCE_RANKING", "CLAIMS", "SYNTHESIS", "GATE_DECISION");
        if (reasonCode.contains("CLAIM") || reasonCode.contains("CONTRADICT")) return List.of("CLAIMS", "SYNTHESIS", "GATE_DECISION");
        if ("NO_VIABLE_SUBNICHE".equals(gateDecision)) return List.of("CANDIDATE_FINALIST", "QUERY_PLAN", "GATE_DECISION");
        return List.of("QUERY_PLAN", "SOURCE_RANKING", "CLAIMS", "GATE_DECISION");
    }

    /** Converte valor flexível para inteiro com padrão seguro. */
    private int integer(Object value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ex) { return fallback; }
    }

    /** Converte valor flexível para decimal com padrão seguro. */
    private double decimal(Object value, double fallback) {
        try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ex) { return fallback; }
    }

    /** Retorna texto seguro para decisões determinísticas. */
    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}

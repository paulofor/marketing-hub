package com.marketinghub.nichocnaev2.pipeline.candidatetournament;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compara candidatos por densidade e qualidade de evidências antes de escolher finalistas do NichoCNAE v2. */
public final class CandidateTournamentProcessor implements StageProcessor {
    private static final int MAX_FINALISTS = 2;
    private static final double MIN_VIABLE_SCORE = 3.0;

    /** Seleciona até dois finalistas usando somente sinais observados recebidos no snapshot de entrada. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> candidates = mapList(context.input().get("candidates"));
        if (candidates.isEmpty()) {
            candidates = mapList(context.input().get("candidateEvidence"));
        }
        List<Map<String, Object>> ranked = candidates.stream()
                .map(this::scoreCandidate)
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();
        List<Map<String, Object>> finalists = ranked.stream()
                .filter(candidate -> scoreOf(candidate) >= MIN_VIABLE_SCORE)
                .limit(MAX_FINALISTS)
                .toList();
        String decision = finalists.isEmpty() ? "NO_VIABLE_SUBNICHE" : "FINALISTS_SELECTED";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "candidate-tournament");
        output.put("tournamentDecision", decision);
        output.put("candidateCount", ranked.size());
        output.put("finalistCount", finalists.size());
        output.put("rankedCandidates", ranked);
        output.put("finalists", finalists);
        output.put("gateDecision", decision);
        output.put("reasonCode", finalists.isEmpty() ? "NO_VIABLE_SUBNICHE" : "FINALISTS_SELECTED");
        output.put("nextStageCode", finalists.isEmpty() ? "reprocess-controller" : "source-fetcher-reranker");
        return new StageResult(decision, output, List.of(new StageArtifact(
                "CANDIDATE_TOURNAMENT",
                "inline://candidate-tournament/ranking",
                "Ranking auditável baseado em evidências observadas, sem opinião prévia do modelo.")));
    }

    /** Calcula score comercial conservador a partir de evidências diretas, independentes e riscos observados. */
    private Map<String, Object> scoreCandidate(Map<String, Object> candidate) {
        Map<String, Object> scored = new LinkedHashMap<>(candidate);
        int directEvidenceCount = number(candidate, "directEvidenceCount", "acceptedClaimCount", "evidenceCount");
        int independentSourceCount = number(candidate, "independentSourceCount", "sourceCount");
        int rejectedSourceCount = number(candidate, "rejectedSourceCount", "unsafeSourceCount");
        int contradictionCount = number(candidate, "contradictionCount");
        double densityScore = Math.min(2.0, directEvidenceCount * 0.7);
        double independenceScore = Math.min(2.0, independentSourceCount * 0.8);
        double riskPenalty = (rejectedSourceCount * 0.4) + (contradictionCount * 0.8);
        double tournamentScore = Math.max(0.0, densityScore + independenceScore - riskPenalty);
        scored.put("tournamentScore", tournamentScore);
        scored.put(
                "tournamentRationale",
                rationale(directEvidenceCount, independentSourceCount, rejectedSourceCount, contradictionCount));
        return scored;
    }

    /** Monta justificativa curta para auditoria da decisão do torneio. */
    private String rationale(
            int directEvidenceCount, int independentSourceCount, int rejectedSourceCount, int contradictionCount) {
        return "evidenciasDiretas=" + directEvidenceCount
                + "; fontesIndependentes=" + independentSourceCount
                + "; fontesRejeitadas=" + rejectedSourceCount
                + "; contradicoes=" + contradictionCount;
    }

    /** Extrai o primeiro número inteiro disponível entre aliases de contrato. */
    private int number(Map<String, Object> candidate, String... keys) {
        for (String key : keys) {
            Object value = candidate.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null && String.valueOf(value).matches("-?\\d+")) {
                return Integer.parseInt(String.valueOf(value));
            }
        }
        return 0;
    }

    /** Lê listas de mapas sem acoplar o processor a DTO específico do backend. */
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, val) -> copy.put(String.valueOf(key), val));
                mapped.add(copy);
            }
        }
        return mapped;
    }

    /** Lê o score calculado sem depender de conversão externa. */
    private double scoreOf(Map<String, Object> candidate) {
        Object value = candidate.get("tournamentScore");
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}

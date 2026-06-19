package com.marketinghub.nichocnaev2.pipeline.knowledgeaccumulator;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Consolida o conhecimento versionado do ciclo NichoCNAE v2 sem criar fatos novos. */
public final class KnowledgeAccumulatorProcessor implements StageProcessor {
    private static final Set<String> VALIDATED_STATES = Set.of("ACCEPT", "ACCEPTED", "VALIDATED");
    private static final Set<String> REJECTED_STATES = Set.of("REJECT", "REJECTED", "STALE", "SUPERSEDED");

    /** Gera um snapshot auditável com fatos aceitos, rejeições, gaps e memória para reprocessamento. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> input = context.input();
        int previousKnowledgeVersion = integer(input.get("knowledgeVersion"), 0);
        List<Map<String, Object>> candidates = mapList(input.get("candidates"));
        List<Map<String, Object>> claims = firstNonEmpty(input, "claims", "validatedClaims", "evidenceClaims");
        List<Map<String, Object>> sources = firstNonEmpty(input, "sources", "selectedSources", "rankedSources");
        List<Map<String, Object>> queries = firstNonEmpty(input, "queries", "executedQueries", "queryMemory");
        List<Map<String, Object>> failures = firstNonEmpty(input, "failures", "previousFailures", "failureSignatures");
        List<Map<String, Object>> decisions = firstNonEmpty(input, "gateDecisions", "decisions");

        List<Map<String, Object>> validatedFacts = validatedFacts(claims);
        List<Map<String, Object>> rejectedClaims = rejectedClaims(claims);
        List<Map<String, Object>> acceptedSources = acceptedSources(sources);
        List<Map<String, Object>> rejectedSources = rejectedSources(sources);
        List<String> evidenceGaps = evidenceGaps(input, validatedFacts, acceptedSources);
        int knowledgeVersion = previousKnowledgeVersion + 1;

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("researchCycleId", input.get("researchCycleId"));
        snapshot.put("candidateId", input.get("candidateId"));
        snapshot.put("candidateVersion", integer(input.get("candidateVersion"), 1));
        snapshot.put("knowledgeVersion", knowledgeVersion);
        snapshot.put("validatedFacts", validatedFacts);
        snapshot.put("acceptedSources", acceptedSources);
        snapshot.put("rejectedSources", rejectedSources);
        snapshot.put("rejectedClaims", rejectedClaims);
        snapshot.put("queriesAlreadyExecuted", queryMemory(queries));
        snapshot.put("failureSignatures", failureSignatures(failures));
        snapshot.put("gateDecisions", decisions);
        snapshot.put("evidenceGaps", evidenceGaps);
        snapshot.put("artifactLineage", artifactLineage(input));
        snapshot.put("budgetConsumed", input.getOrDefault("budgetConsumed", 0));
        snapshot.put("epistemicSummary", epistemicSummary(validatedFacts, rejectedClaims, acceptedSources, evidenceGaps));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "knowledge-accumulator");
        output.put("knowledgeVersion", knowledgeVersion);
        output.put("snapshot", snapshot);
        output.put("validatedFactCount", validatedFacts.size());
        output.put("acceptedSourceCount", acceptedSources.size());
        output.put("rejectedSourceCount", rejectedSources.size());
        output.put("evidenceGaps", evidenceGaps);
        output.put("nextStageCode", "reprocess-controller");
        return new StageResult("KNOWLEDGE_SNAPSHOT_READY", output, List.of(new StageArtifact(
                "KNOWLEDGE_SNAPSHOT",
                "inline://knowledge-accumulator/snapshot/v" + knowledgeVersion,
                "Snapshot versionado com conhecimento validado, rejeições, memória de queries/fontes e gaps acionáveis.")));
    }

    /** Seleciona claims aceitos com trecho exato para virar fato validado. */
    private List<Map<String, Object>> validatedFacts(List<Map<String, Object>> claims) {
        return claims.stream()
                .filter(claim -> VALIDATED_STATES.contains(stateOf(claim)))
                .filter(claim -> !text(claim.get("exactEvidenceSpan")).isBlank())
                .map(this::validatedFact)
                .toList();
    }

    /** Converte claim validado em fato canônico sem inventar conteúdo novo. */
    private Map<String, Object> validatedFact(Map<String, Object> claim) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("canonicalClaimId", claim.getOrDefault("canonicalClaimId", claim.get("claimId")));
        fact.put("claimType", claim.get("claimType"));
        fact.put("claimText", claim.getOrDefault("claimText", claim.get("canonicalClaim")));
        fact.put("exactEvidenceSpan", claim.get("exactEvidenceSpan"));
        fact.put("sourceUrl", claim.get("sourceUrl"));
        fact.put("canonicalDomain", domainOf(text(claim.get("sourceUrl")), text(claim.get("canonicalDomain"))));
        fact.put("epistemicState", "VALIDATED");
        return fact;
    }

    /** Preserva claims rejeitados ou contraditórios como diagnóstico, não como sinal positivo. */
    private List<Map<String, Object>> rejectedClaims(List<Map<String, Object>> claims) {
        return claims.stream()
                .filter(claim -> REJECTED_STATES.contains(stateOf(claim)) || isContradictory(claim))
                .map(claim -> Map.<String, Object>of(
                        "claimId", claim.getOrDefault("claimId", ""),
                        "claimType", claim.getOrDefault("claimType", ""),
                        "reason", claim.getOrDefault("rejectionReason", claim.getOrDefault("rejectionReasons", "REJECTED_OR_CONTRADICTED")),
                        "epistemicState", isContradictory(claim) ? "CONTRADICTED" : stateOf(claim)))
                .toList();
    }

    /** Lista fontes aceitas com domínio canônico para reuso seguro. */
    private List<Map<String, Object>> acceptedSources(List<Map<String, Object>> sources) {
        return sources.stream().filter(source -> !Boolean.TRUE.equals(source.get("unsafe")) && text(source.get("rejectionReason")).isBlank())
                .map(source -> sourceMemory(source, "ACCEPTED"))
                .toList();
    }

    /** Lista fontes rejeitadas para evitar repetição de coleta e custo. */
    private List<Map<String, Object>> rejectedSources(List<Map<String, Object>> sources) {
        return sources.stream().filter(source -> Boolean.TRUE.equals(source.get("unsafe")) || !text(source.get("rejectionReason")).isBlank())
                .map(source -> sourceMemory(source, "REJECTED"))
                .toList();
    }

    /** Converte uma fonte em memória auditável mínima. */
    private Map<String, Object> sourceMemory(Map<String, Object> source, String status) {
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("url", source.get("url"));
        memory.put("canonicalDomain", domainOf(text(source.get("url")), text(source.get("canonicalDomain"))));
        memory.put("contentHash", source.get("contentHash"));
        memory.put("status", status);
        memory.put("reason", source.getOrDefault("rejectionReason", source.get("rerankRationale")));
        return memory;
    }

    /** Consolida memória de queries já executadas por hash textual simples. */
    private List<Map<String, Object>> queryMemory(List<Map<String, Object>> queries) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> memory = new ArrayList<>();
        for (Map<String, Object> query : queries) {
            String text = text(query.getOrDefault("query", query.get("queryText")));
            String hash = normalize(text);
            if (!hash.isBlank() && seen.add(hash)) {
                memory.add(Map.of("queryText", text, "queryHash", hash, "resultCount", integer(query.get("resultCount"), 0)));
            }
        }
        return memory;
    }

    /** Consolida assinaturas de falhas anteriores para impedir loops cognitivos. */
    private List<Map<String, Object>> failureSignatures(List<Map<String, Object>> failures) {
        return failures.stream()
                .map(failure -> Map.<String, Object>of(
                        "stage", failure.getOrDefault("stage", failure.getOrDefault("stageCode", "")),
                        "reasonCode", failure.getOrDefault("reasonCode", failure.getOrDefault("failureType", "")),
                        "signature", normalize(text(failure.getOrDefault("signature", failure.get("errorMessage"))))))
                .toList();
    }

    /** Calcula gaps acionáveis a partir do snapshot recebido e dos fatos realmente validados. */
    private List<String> evidenceGaps(Map<String, Object> input, List<Map<String, Object>> facts, List<Map<String, Object>> sources) {
        Set<String> gaps = new LinkedHashSet<>(textList(input.get("evidenceGaps")));
        if (facts.stream().noneMatch(fact -> typeOf(fact).contains("TASK") || typeOf(fact).contains("ROUTINE"))) {
            gaps.add("CONCRETE_EXECUTOR_TASKS");
        }
        if (facts.stream().noneMatch(fact -> typeOf(fact).contains("PAIN") || typeOf(fact).contains("FAILURE"))) {
            gaps.add("DISTINCT_RECURRING_PAINS");
        }
        if (facts.stream().noneMatch(fact -> typeOf(fact).contains("ECONOMIC") || typeOf(fact).contains("COST") || typeOf(fact).contains("WORKAROUND"))) {
            gaps.add("ECONOMIC_IMPACT_OR_WORKAROUND");
        }
        long independentDomains = sources.stream().map(source -> text(source.get("canonicalDomain"))).filter(domain -> !domain.isBlank()).distinct().count();
        if (independentDomains < 3) {
            gaps.add("THREE_INDEPENDENT_DOMAINS");
        }
        return List.copyOf(gaps);
    }

    /** Registra linhagem mínima dos artefatos recebidos sem inspecionar infraestrutura externa. */
    private List<Map<String, Object>> artifactLineage(Map<String, Object> input) {
        return mapList(input.get("artifactLineage")).isEmpty() ? List.of() : mapList(input.get("artifactLineage"));
    }

    /** Resume o estado epistêmico do snapshot para a tela e para o controlador de reprocessamento. */
    private Map<String, Object> epistemicSummary(List<Map<String, Object>> facts, List<Map<String, Object>> rejected, List<Map<String, Object>> sources, List<String> gaps) {
        return Map.of(
                "validatedFactCount", facts.size(),
                "rejectedClaimCount", rejected.size(),
                "acceptedSourceCount", sources.size(),
                "openGapCount", gaps.size(),
                "readyForReprocessController", true);
    }

    /** Extrai a primeira lista de mapas disponível entre chaves alternativas. */
    private List<Map<String, Object>> firstNonEmpty(Map<String, Object> input, String... keys) {
        for (String key : keys) {
            List<Map<String, Object>> values = mapList(input.get(key));
            if (!values.isEmpty()) return values;
        }
        return List.of();
    }

    /** Extrai lista de mapas de contratos flexíveis. */
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> items)) return List.of();
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

    /** Extrai strings normalizadas de lista flexível. */
    private List<String> textList(Object value) {
        if (!(value instanceof List<?> items)) return List.of();
        return items.stream().map(this::text).filter(item -> !item.isBlank()).toList();
    }

    /** Identifica o estado epistêmico/status do claim. */
    private String stateOf(Map<String, Object> claim) {
        String state = text(claim.get("epistemicState")).toUpperCase(Locale.ROOT);
        return state.isBlank() ? text(claim.get("status")).toUpperCase(Locale.ROOT) : state;
    }

    /** Identifica contradição explícita. */
    private boolean isContradictory(Map<String, Object> claim) {
        return "CONTRADICTED".equals(stateOf(claim)) || Boolean.TRUE.equals(claim.get("contradictsTarget"));
    }

    /** Retorna tipo textual do fato. */
    private String typeOf(Map<String, Object> fact) {
        return text(fact.get("claimType")).toUpperCase(Locale.ROOT);
    }

    /** Converte número flexível para inteiro seguro. */
    private int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ex) { return fallback; }
    }

    /** Retorna texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Normaliza texto para assinatura determinística simples. */
    private String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim();
    }

    /** Descobre domínio canônico preservando valor já informado pelo contrato. */
    private String domainOf(String url, String fallback) {
        if (!fallback.isBlank()) return fallback;
        try {
            String host = new URI(url).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (URISyntaxException ex) {
            return "";
        }
    }
}

package com.marketinghub.nichocnaev2.pipeline.sourcefetcherreranker;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Coleta snapshots já recebidos e prioriza fontes diretas, independentes e alinhadas ao gate do NichoCNAE v2. */
public final class SourceFetcherRerankerProcessor implements StageProcessor {
    private static final int MAX_SELECTED_SOURCES = 8;
    private static final int MAX_SOURCES_PER_DOMAIN = 2;

    /** Seleciona fontes úteis, deduplicadas e rastreáveis sem acessar banco ou decidir avanço fora do executor. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> sources = mapList(context.input().get("sources"));
        if (sources.isEmpty()) {
            sources = mapList(context.input().get("sourceCandidates"));
        }
        Set<String> fetchedContentHashes = new LinkedHashSet<>(
                normalizedTextList(context.input().get("fetchedContentHashes")));
        Map<String, Integer> selectedByDomain = new LinkedHashMap<>();
        List<Map<String, Object>> rankedSources = sources.stream()
                .map(source -> scoreSource(source, fetchedContentHashes))
                .sorted(Comparator.comparingDouble(this::scoreOf).reversed())
                .toList();
        List<Map<String, Object>> selectedSources = new ArrayList<>();
        List<Map<String, Object>> rejectedSources = new ArrayList<>();
        for (Map<String, Object> source : rankedSources) {
            String domain = text(source.get("canonicalDomain"));
            if (domain.isBlank()) {
                domain = domainOf(text(source.get("url")));
            }
            boolean duplicate = Boolean.TRUE.equals(source.get("duplicateContent"));
            boolean unsafe = Boolean.TRUE.equals(source.get("unsafe"));
            boolean domainQuotaExceeded = selectedByDomain.getOrDefault(domain, 0) >= MAX_SOURCES_PER_DOMAIN;
            if (!duplicate && !unsafe && !domainQuotaExceeded && selectedSources.size() < MAX_SELECTED_SOURCES) {
                selectedSources.add(source);
                selectedByDomain.put(domain, selectedByDomain.getOrDefault(domain, 0) + 1);
            } else {
                Map<String, Object> rejected = new LinkedHashMap<>(source);
                rejected.put("rejectionReason", rejectionReason(duplicate, unsafe, domainQuotaExceeded));
                rejectedSources.add(rejected);
            }
        }
        String decision = selectedSources.isEmpty() ? "NO_FETCHABLE_DIRECT_SOURCE" : "SOURCES_SELECTED";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-fetcher-reranker");
        output.put("sourceFetchDecision", decision);
        output.put("fetchedSnapshotCount", rankedSources.size());
        output.put("selectedSourceCount", selectedSources.size());
        output.put("rejectedSourceCount", rejectedSources.size());
        output.put("rankedSources", rankedSources);
        output.put("selectedSources", selectedSources);
        output.put("rejectedSources", rejectedSources);
        output.put("nextStageCode", selectedSources.isEmpty() ? "adaptive-query-planner" : "signal-extractor");
        return new StageResult(decision, output, List.of(new StageArtifact(
                "SOURCE_FETCH_RERANKING",
                "inline://source-fetcher-reranker/selection",
                "Snapshots priorizados por prova direta, independência de domínio e alinhamento ao objetivo do gate.")));
    }

    /** Calcula prioridade conservadora privilegiando fonte direta, aderência de ator/contexto e independência. */
    private Map<String, Object> scoreSource(Map<String, Object> source, Set<String> fetchedContentHashes) {
        Map<String, Object> scored = new LinkedHashMap<>(source);
        String contentHash = normalize(text(source.get("contentHash")));
        boolean duplicateContent = !contentHash.isBlank() && fetchedContentHashes.contains(contentHash);
        boolean unsafe = Boolean.TRUE.equals(source.get("unsafe")) || containsUnsafeMarker(source);
        double directness = directnessScore(text(source.get("sourceDirectness")), text(source.get("sourceType")));
        double actor = decimal(source, "actorMatch", "targetActorMatch");
        double context = decimal(source, "contextMatch", "jobContextMatch");
        double objective = objectiveScore(source.get("supportedGoals"));
        double penalty = (duplicateContent ? 2.0 : 0.0) + (unsafe ? 3.0 : 0.0) + decimal(source, "contaminationRisk");
        double score = Math.max(0.0, directness + actor + context + objective - penalty);
        scored.put("canonicalDomain", domainOf(text(source.get("url"))));
        scored.put("duplicateContent", duplicateContent);
        scored.put("unsafe", unsafe);
        scored.put("rerankScore", score);
        scored.put(
                "rerankRationale",
                "direta=" + directness
                        + "; ator=" + actor
                        + "; contexto=" + context
                        + "; objetivo=" + objective
                        + "; penalidade=" + penalty);
        return scored;
    }

    /** Converte classificação textual de fonte em peso de valor marginal para fetch. */
    private double directnessScore(String sourceDirectness, String sourceType) {
        String joined = (sourceDirectness + " " + sourceType).toUpperCase(Locale.ROOT);
        if (joined.contains("FIRST_PERSON") || joined.contains("DIRECT")) {
            return 2.0;
        }
        if (joined.contains("INSTITUTIONAL") || joined.contains("MARKETPLACE")) {
            return 1.0;
        }
        if (joined.contains("ANALOGY")) {
            return 0.0;
        }
        return 0.5;
    }

    /** Mede se a fonte atende objetivos funcionais do gate, sem criar nova regra de negócio no backend. */
    private double objectiveScore(Object supportedGoals) {
        List<String> goals = textList(supportedGoals);
        double score = 0.0;
        if (goals.contains("ROUTINE")) {
            score += 0.8;
        }
        if (goals.contains("PAIN")) {
            score += 0.8;
        }
        if (goals.contains("ECONOMIC_IMPACT") || goals.contains("HIRING_BEHAVIOR")) {
            score += 0.6;
        }
        return score;
    }

    /** Identifica marcadores de risco de segurança já enviados pela etapa anterior. */
    private boolean containsUnsafeMarker(Map<String, Object> source) {
        String text = normalize(text(source.get("safetyDecision")) + " " + text(source.get("riskLevel")));
        return text.contains("unsafe") || text.contains("blocked") || text.contains("adult");
    }

    /** Explica por que uma fonte ficou fora da seleção final. */
    private String rejectionReason(boolean duplicate, boolean unsafe, boolean domainQuotaExceeded) {
        if (unsafe) {
            return "UNSAFE_SOURCE";
        }
        if (duplicate) {
            return "DUPLICATE_CONTENT";
        }
        if (domainQuotaExceeded) {
            return "DOMAIN_QUOTA_EXCEEDED";
        }
        return "LOW_MARGINAL_VALUE";
    }

    /** Extrai lista de mapas de contratos flexíveis. */
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

    /** Extrai lista textual de contratos flexíveis. */
    private List<String> textList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null)
                .map(item -> String.valueOf(item).trim().toUpperCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .toList();
    }

    /** Extrai lista textual normalizada sem alterar a semântica de hashes e URLs. */
    private List<String> normalizedTextList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null)
                .map(item -> normalize(String.valueOf(item)))
                .filter(item -> !item.isBlank())
                .toList();
    }

    /** Lê número decimal entre aliases sem exigir DTO específico. */
    private double decimal(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.parseDouble(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    return 0.0;
                }
            }
        }
        return 0.0;
    }

    /** Retorna texto seguro para normalização. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Normaliza texto para comparações simples de marcadores. */
    private String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    /** Extrai domínio canônico básico da URL para cotas de independência. */
    private String domainOf(String url) {
        if (url.isBlank()) {
            return "";
        }
        try {
            String host = new URI(url).getHost();
            if (host == null) {
                return "";
            }
            return host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (URISyntaxException ex) {
            return "";
        }
    }

    /** Lê o score de reranking calculado. */
    private double scoreOf(Map<String, Object> source) {
        Object value = source.get("rerankScore");
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}

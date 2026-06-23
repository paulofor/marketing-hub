package com.marketinghub.nichocnaev2.pipeline.adaptivequeryplanner;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Planeja buscas adaptativas por gaps reais de conhecimento do pipeline NichoCNAE versão 2. */
public final class AdaptiveQueryPlannerProcessor implements StageProcessor {
    private static final int MAX_QUERIES = 8;

    /** Cria um plano curto de pesquisa, reutilizando memória e evitando repetir queries ou fontes anteriores. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> input = context.input();
        List<String> gaps = textList(input.get("evidenceGaps"));
        if (gaps.isEmpty()) {
            gaps = textList(input.get("gaps"));
        }
        if (gaps.isEmpty()) {
            gaps = instagramDiscoveryGaps(input);
        }
        String audience = firstText(input, "audience", "targetAudience", "neutralCandidateName", "audienceFocus");
        String jobContext = firstText(input, "jobContext", "operationalJob", "context", "operationalContext", "cnaeDescription");
        Set<String> previousQueryHashes = new LinkedHashSet<>(textList(input.get("previousQueryHashes")));
        List<Map<String, Object>> plannedQueries = new ArrayList<>();
        int skipped = 0;
        for (String gap : gaps) {
            for (String query : candidateQueries(audience, jobContext, gap)) {
                String hash = queryHash(query);
                if (previousQueryHashes.contains(hash) || containsQuery(plannedQueries, hash)) {
                    skipped++;
                    continue;
                }
                plannedQueries.add(plannedQuery(query, gap, hash));
                if (plannedQueries.size() >= MAX_QUERIES) {
                    break;
                }
            }
            if (plannedQueries.size() >= MAX_QUERIES) {
                break;
            }
        }
        String decision = plannedQueries.isEmpty() ? "NO_RESEARCH_GAIN" : "PLAN_READY";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "adaptive-query-planner");
        output.put("planDecision", decision);
        output.put("plannedQueryCount", plannedQueries.size());
        output.put("reusedQueryCount", previousQueryHashes.size());
        output.put("skippedQueryCount", skipped);
        output.put("plannedQueries", plannedQueries);
        output.put("earlyStopping", plannedQueries.isEmpty());
        output.put("nextStageCode", plannedQueries.isEmpty() ? "candidate-tournament" : "source-searcher");
        return new StageResult(decision, output, List.of(new StageArtifact(
                "QUERY_PLAN", "inline://adaptive-query-planner/plan", "Plano curto orientado aos gaps ainda não provados.")));
    }

    /** Gera variações naturais de busca a partir do público, contexto e lacuna de evidência. */
    private List<String> candidateQueries(String audience, String jobContext, String gap) {
        String compactAudience = audience.isBlank() ? "profissional autonomo" : audience;
        String compactContext = jobContext.isBlank() ? compactAudience : jobContext;
        String compactGap = normalizeGap(gap);
        return List.of(
                compactAudience + " " + compactGap + " instagram",
                compactContext + " " + compactGap + " whatsapp",
                compactAudience + " relatos " + compactGap + " autonomo",
                compactAudience + " rotina " + compactGap + " mei");
    }

    /** Cria lacunas amplas quando a entrada ainda não trouxe gaps, evitando parar antes de testar público de Instagram. */
    private List<String> instagramDiscoveryGaps(Map<String, Object> input) {
        if (!mapList(input.get("candidates")).isEmpty() || !mapList(input.get("finalists")).isEmpty()) {
            return List.of(
                    "conseguir clientes pelo instagram",
                    "whatsapp agenda vazia",
                    "cobrar preço justo",
                    "ganhar dinheiro como autonomo",
                    "medo de ficar sem cliente");
        }
        return List.of();
    }

    /** Normaliza o gap para produzir termos curtos, pesquisáveis e sem linguagem de oferta. */
    private String normalizeGap(String gap) {
        String normalized = String.valueOf(gap == null ? "" : gap).trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('_', ' ').replace('-', ' ');
        normalized = normalized.replaceAll("(?i)\\b(produto|oferta|curso|landing|campanha|promessa)\\b", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? "dificuldade rotina" : normalized;
    }

    /** Monta o item estruturado da query planejada para auditoria e próxima etapa. */
    private Map<String, Object> plannedQuery(String query, String gap, String hash) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("query", query);
        item.put("queryHash", hash);
        item.put("gap", gap);
        item.put("objective", "EVIDENCE_GAP");
        item.put("maxResults", 5);
        return item;
    }

    /** Verifica duplicidade dentro do próprio plano em construção. */
    private boolean containsQuery(List<Map<String, Object>> plannedQueries, String hash) {
        return plannedQueries.stream().anyMatch(query -> hash.equals(query.get("queryHash")));
    }

    /** Extrai uma lista textual de contratos flexíveis do snapshot recebido do backend. */
    private List<String> textList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !String.valueOf(item).isBlank())
                .map(String::valueOf)
                .toList();
    }

    /** Lê listas de mapas para reconhecer contratos de candidatos sem depender de DTO específico. */
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

    /** Retorna o primeiro campo textual não vazio entre aliases de contrato. */
    private String firstText(Map<String, Object> input, String... keys) {
        for (String key : keys) {
            Object value = input.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    /** Gera hash estável simples para impedir repetição de queries já tentadas. */
    private String queryHash(String query) {
        String normalized = Normalizer.normalize(query.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim();
        return Integer.toHexString(normalized.hashCode());
    }
}

package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.net.URI;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Processa a etapa source-searcher do pipeline NichoCNAE v3. */
public final class SourceSearcherProcessor implements StageProcessor {
    private static final int SEARCH_RESULTS_PER_QUERY = 5;
    private static final int MAX_SELECTED_SOURCES = 8;
    private static final int MIN_ROUTINE_EVIDENCE_SCORE = 45;
    private static final SourceSearchClient EMPTY_SEARCH_CLIENT = (query, limit) -> List.of();

    private final SourceSearchClient searchClient;

    /** Inicializa o processor com cliente vazio para testes e execução sem integração externa. */
    public SourceSearcherProcessor() {
        this(EMPTY_SEARCH_CLIENT);
    }

    /** Inicializa o processor com cliente de busca pública rastreável. */
    public SourceSearcherProcessor(SourceSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /** Executa a etapa source-searcher buscando e qualificando fontes reais antes de permitir snapshots. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> foundSources = maps(context.input().get("foundSources"));
        if (foundSources.isEmpty()) {
            foundSources = maps(context.input().get("selectedSources"));
        }
        List<Map<String, Object>> plannedQueries = maps(context.input().get("plannedQueries"));
        List<Map<String, Object>> searchAttempts = List.of();
        if (foundSources.isEmpty()) {
            SearchOutput searchOutput = search(plannedQueries);
            foundSources = searchOutput.selectedSources();
            searchAttempts = searchOutput.searchAttempts();
        } else {
            foundSources = qualifyExistingSources(foundSources);
        }
        boolean hasSources = !foundSources.isEmpty();
        String status = hasSources ? "FONTES_ENCONTRADAS" : "FONTES_NAO_COLETADAS";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-searcher");
        output.put("status", status);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("plannedQueries", plannedQueries);
        output.put("foundSourceCount", foundSources.size());
        output.put("foundSources", foundSources);
        output.put("selectedSources", foundSources);
        output.put("searchAttempts", searchAttempts);
        output.put("selectionCriteria", List.of(
                "fonte Brasil-first em português quando possível",
                "fonte descreve rotina, tarefa, dúvida, atendimento, agenda, cobrança ou execução real",
                "fonte comercial ou de solução não avança como evidência positiva",
                "fontes com URL duplicada ou sem URL rastreável são descartadas"));
        output.put("blocked", !hasSources);
        output.put("decisionReason", hasSources
                ? "Fontes públicas qualificadas disponíveis para coleta de snapshots auditáveis."
                : "A etapa source-searcher não encontrou fontes públicas qualificadas de rotina; não é seguro avançar com queries ou fonte comercial.");
        output.put("recommendedCorrectionStage", hasSources ? "" : "source-searcher");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", hasSources ? "source-fetcher" : "");
        return new StageResult(status, output, List.of(new StageArtifact(status, "inline://nichocnae-v3/source-searcher", hasSources
                ? "Fontes públicas qualificadas encontradas para coleta de snapshots."
                : "Busca de fontes bloqueada por ausência de fontes públicas qualificadas.")));
    }

    /** Converte uma lista de objetos em mapas estruturados. */
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    ((Map<?, ?>) item).forEach((key, val) -> normalized.put(String.valueOf(key), val));
                    return normalized;
                })
                .toList();
    }

    /** Executa as queries planejadas e seleciona fontes qualificadas para a próxima etapa. */
    private SearchOutput search(List<Map<String, Object>> plannedQueries) {
        Set<String> seenUrls = new HashSet<>();
        List<Map<String, Object>> attempts = plannedQueries.stream()
                .map(query -> searchOneQuery(query, seenUrls))
                .toList();
        List<Map<String, Object>> selectedSources = attempts.stream()
                .flatMap(attempt -> maps(attempt.get("qualifiedSources")).stream())
                .sorted(Comparator
                        .comparingInt((Map<String, Object> source) -> score(source, "qualityScore")).reversed()
                        .thenComparing(source -> text(source.get("url"))))
                .limit(MAX_SELECTED_SOURCES)
                .toList();
        return new SearchOutput(selectedSources, attempts);
    }

    /** Busca uma query e registra tentativa com quantidade bruta e fontes aceitas. */
    private Map<String, Object> searchOneQuery(Map<String, Object> plannedQuery, Set<String> seenUrls) {
        String originalQuery = text(plannedQuery.get("query"));
        String query = enrichBrazilFirstQuery(originalQuery);
        List<SourceSearchResult> rawResults = query.isBlank() ? List.of() : searchClient.search(query, SEARCH_RESULTS_PER_QUERY);
        List<Map<String, Object>> qualifiedSources = rawResults.stream()
                .map(result -> qualify(result, plannedQuery))
                .filter(source -> !text(source.get("url")).isBlank())
                .filter(source -> seenUrls.add(text(source.get("url"))))
                .filter(this::isQualifiedRoutineSource)
                .toList();
        Map<String, Object> attempt = new LinkedHashMap<>();
        attempt.put("query", query);
        attempt.put("originalQuery", originalQuery);
        attempt.put("intent", text(plannedQuery.get("intent")));
        attempt.put("objective", text(plannedQuery.get("objective")));
        attempt.put("rawResultCount", rawResults.size());
        attempt.put("qualifiedSourceCount", qualifiedSources.size());
        attempt.put("qualifiedSources", qualifiedSources);
        return attempt;
    }

    /** Qualifica fontes já recebidas por contrato legado ou reprocessamento. */
    private List<Map<String, Object>> qualifyExistingSources(List<Map<String, Object>> sources) {
        return sources.stream()
                .map(source -> qualify(source, source))
                .filter(this::isQualifiedRoutineSource)
                .limit(MAX_SELECTED_SOURCES)
                .toList();
    }

    /** Monta fonte qualificada a partir de resultado público bruto. */
    private Map<String, Object> qualify(SourceSearchResult result, Map<String, Object> plannedQuery) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("url", result.url());
        source.put("title", result.title());
        source.put("snippet", result.snippet());
        source.put("searchProvider", result.provider());
        source.put("matchedQuery", text(plannedQuery.get("query")));
        source.put("queryIntent", text(plannedQuery.get("intent")));
        source.put("routineRelevance", text(plannedQuery.get("objective")));
        return qualify(source, plannedQuery);
    }

    /** Calcula classificação de rotina, Brasil e risco comercial para uma fonte. */
    private Map<String, Object> qualify(Map<String, Object> source, Map<String, Object> plannedQuery) {
        Map<String, Object> qualified = new LinkedHashMap<>(source);
        String title = text(first(qualified, "title", "sourceTitle", "name"));
        String url = text(first(qualified, "url", "sourceUrl", "link"));
        String snippet = text(first(qualified, "snippet", "excerpt", "evidence", "description"));
        String combined = (title + " " + snippet + " " + url).toLowerCase();
        int routineEvidenceScore = routineEvidenceScore(combined);
        int brazilRelevanceScore = brazilRelevanceScore(url, combined);
        boolean solutionLanguageRisk = containsAny(combined, List.of("software", "sistema", "app", "aplicativo", "plataforma", "automação", "automacao", "curso", "mentoria", "template", "funil", "landing page", "tráfego pago", "trafego pago", "crm"));
        boolean commercialPageRisk = containsAny(combined, List.of("preço", "preco", "planos", "contrate", "comprar", "teste grátis", "teste gratis", "demonstração", "demonstracao", "vendas", "marketing digital", "anúncio", "anuncio"));
        boolean structuredBusinessDriftRisk = containsAny(combined, List.of("empresa", "indústria", "industria", "corporativo", "franquia", "grande porte", "gestão empresarial"));
        int qualityScore = routineEvidenceScore + brazilRelevanceScore - (solutionLanguageRisk ? 35 : 0) - (commercialPageRisk ? 20 : 0) - (structuredBusinessDriftRisk ? 15 : 0);
        String sourceIntent = solutionLanguageRisk || commercialPageRisk
                ? "CONTAMINATION_RISK"
                : "ROUTINE_EVIDENCE";

        qualified.put("url", url);
        qualified.put("title", title);
        qualified.put("snippet", snippet);
        qualified.put("sourceIntent", sourceIntent);
        qualified.put("sourceType", sourceType(url, combined, sourceIntent));
        qualified.put("routineEvidenceScore", Math.max(0, Math.min(100, routineEvidenceScore)));
        qualified.put("brazilRelevanceScore", Math.max(0, Math.min(100, brazilRelevanceScore)));
        qualified.put("autonomousProfessionalEvidenceScore", autonomousProfessionalEvidenceScore(combined));
        qualified.put("sourceFreshnessScore", sourceFreshnessScore(combined));
        qualified.put("outdatedSourceRisk", containsAny(combined, List.of("2019", "2018", "2017", "2016", "2015")));
        qualified.put("commercialPageRisk", commercialPageRisk);
        qualified.put("solutionLanguageRisk", solutionLanguageRisk);
        qualified.put("structuredBusinessDriftRisk", structuredBusinessDriftRisk);
        qualified.put("qualityScore", Math.max(0, qualityScore));
        qualified.putIfAbsent("matchedQuery", text(plannedQuery.get("query")));
        qualified.putIfAbsent("queryIntent", text(plannedQuery.get("intent")));
        qualified.putIfAbsent("routineRelevance", text(plannedQuery.get("objective")));
        return qualified;
    }

    /** Decide se a fonte pode avançar para coleta de snapshot da rotina. */
    private boolean isQualifiedRoutineSource(Map<String, Object> source) {
        return "ROUTINE_EVIDENCE".equals(source.get("sourceIntent"))
                && score(source, "routineEvidenceScore") >= MIN_ROUTINE_EVIDENCE_SCORE
                && score(source, "qualityScore") >= MIN_ROUTINE_EVIDENCE_SCORE;
    }

    /** Enriquece a consulta para priorizar Brasil e rotina real do profissional. */
    private String enrichBrazilFirstQuery(String query) {
        if (query.isBlank()) {
            return "";
        }
        return query + " Brasil MEI autônomo rotina problema atendimento cliente";
    }

    /** Calcula score de evidência de rotina operacional. */
    private int routineEvidenceScore(String text) {
        return scoreByTerms(text, List.of("rotina", "tarefa", "tarefas", "atendimento", "agenda", "cliente", "clientes", "cobrança", "cobranca", "preço", "preco", "orçamento", "orcamento", "material", "estoque", "retrabalho", "problema", "dúvida", "duvida", "pergunta", "frequência", "frequencia", "manual"), 12);
    }

    /** Calcula aderência ao contexto brasileiro. */
    private int brazilRelevanceScore(String url, String text) {
        int score = scoreByTerms(text, List.of("brasil", "brasileiro", "mei", "autônomo", "autonomo", "whatsapp", "instagram", "cliente", "clientes"), 10);
        if (domain(url).endsWith(".br")) {
            score += 35;
        }
        return Math.min(100, score);
    }

    /** Calcula evidência de profissional autônomo ou dono-operador. */
    private int autonomousProfessionalEvidenceScore(String text) {
        return scoreByTerms(text, List.of("mei", "autônomo", "autonomo", "profissional", "dono", "por conta própria", "conta propria", "cliente", "agenda", "whatsapp", "instagram"), 12);
    }

    /** Estima atualidade da fonte a partir de sinais simples presentes no resultado. */
    private int sourceFreshnessScore(String text) {
        if (containsAny(text, List.of("2026", "2025", "2024"))) {
            return 90;
        }
        if (containsAny(text, List.of("2023", "2022"))) {
            return 65;
        }
        return 45;
    }

    /** Classifica o tipo operacional da fonte. */
    private String sourceType(String url, String text, String sourceIntent) {
        if (!"ROUTINE_EVIDENCE".equals(sourceIntent)) {
            return "COMMERCIAL_OR_SOLUTION_RISK";
        }
        if (domain(url).endsWith(".gov.br")) {
            return "BRAZILIAN_OFFICIAL_SOURCE";
        }
        if (containsAny(text, List.of("pergunta", "dúvida", "duvida", "fórum", "forum", "comentário", "comentario"))) {
            return "REAL_PROFESSIONAL_QUESTION";
        }
        if (containsAny(text, List.of("notícia", "noticia", "2026", "2025", "2024"))) {
            return "RECENT_SECTOR_CONTENT";
        }
        return "ROUTINE_PUBLIC_SOURCE";
    }

    /** Calcula score por presença de termos esperados. */
    private int scoreByTerms(String text, List<String> terms, int pointsPerTerm) {
        int score = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                score += pointsPerTerm;
            }
        }
        return Math.min(100, score);
    }

    /** Verifica presença de qualquer termo em texto normalizado. */
    private boolean containsAny(String text, List<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    /** Retorna o primeiro campo existente no mapa de fonte. */
    private Object first(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return "";
    }

    /** Lê um campo numérico de score com fallback zero. */
    private int score(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Extrai domínio simples da URL para classificação Brasil-first. */
    private String domain(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase().replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    /** Converte valor opcional em texto sem nulos. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Agrupa fontes selecionadas e tentativas de busca para auditoria da etapa. */
    private record SearchOutput(List<Map<String, Object>> selectedSources, List<Map<String, Object>> searchAttempts) {}
}

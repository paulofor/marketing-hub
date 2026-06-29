package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Processa a etapa source-searcher do pipeline NichoCNAE v3. */
public final class SourceSearcherProcessor implements StageProcessor {
    private static final int SEARCH_RESULTS_PER_QUERY = 8;
    private static final int MAX_SELECTED_SOURCES = 8;
    private static final int MIN_ROUTINE_EVIDENCE_SCORE = 45;
    private static final int MAX_REJECTED_SOURCES_PER_ATTEMPT = 8;
    private static final Pattern PARENTHETICAL_TEXT = Pattern.compile("\\([^)]*\\)");
    private static final Pattern NON_SEARCH_TEXT = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
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
                "busca ampla separada da classificação de evidência semântica",
                "fonte descreve rotina, tarefa, dúvida, atendimento, canal, agenda, cobrança ou execução real",
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

    /** Busca uma query por variações simples e registra quantidade bruta, fontes aceitas e descartes. */
    private Map<String, Object> searchOneQuery(Map<String, Object> plannedQuery, Set<String> seenUrls) {
        String originalQuery = text(plannedQuery.get("query"));
        List<String> queryVariants = queryVariants(plannedQuery);
        List<SourceSearchResult> rawResults = queryVariants.stream()
                .flatMap(query -> searchClient.search(query, SEARCH_RESULTS_PER_QUERY).stream())
                .toList();
        List<Map<String, Object>> qualifiedSources = new ArrayList<>();
        List<Map<String, Object>> rejectedSources = new ArrayList<>();
        for (SourceSearchResult rawResult : rawResults) {
            Map<String, Object> source = qualify(rawResult, plannedQuery);
            String url = text(source.get("url"));
            String rejectionReason = rejectionReason(source, seenUrls);
            if (rejectionReason.isBlank()) {
                qualifiedSources.add(source);
            } else if (rejectedSources.size() < MAX_REJECTED_SOURCES_PER_ATTEMPT) {
                Map<String, Object> rejected = new LinkedHashMap<>(source);
                rejected.put("rejectionReason", rejectionReason);
                rejectedSources.add(rejected);
            }
            if (!url.isBlank()) {
                seenUrls.add(url);
            }
        }
        Map<String, Object> attempt = new LinkedHashMap<>();
        attempt.put("query", queryVariants.isEmpty() ? "" : queryVariants.getFirst());
        attempt.put("queryVariants", queryVariants);
        attempt.put("originalQuery", originalQuery);
        attempt.put("intent", text(plannedQuery.get("intent")));
        attempt.put("objective", text(plannedQuery.get("objective")));
        attempt.put("rawResultCount", rawResults.size());
        attempt.put("qualifiedSourceCount", qualifiedSources.size());
        attempt.put("qualifiedSources", qualifiedSources);
        attempt.put("rejectedSourceCount", Math.max(0, rawResults.size() - qualifiedSources.size()));
        attempt.put("rejectedSources", rejectedSources);
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
        boolean solutionLanguageRisk = containsAny(combined, List.of("software", "sistema", "aplicativo", "plataforma", "automação", "automacao", "curso", "mentoria", "funil", "landing page", "tráfego pago", "trafego pago", "crm"));
        boolean commercialPageRisk = containsAny(combined, List.of("preço", "preco", "planos", "contrate", "comprar", "teste grátis", "teste gratis", "demonstração", "demonstracao", "vendas", "marketing digital", "anúncio", "anuncio"));
        boolean structuredBusinessDriftRisk = containsAny(combined, List.of("empresa", "indústria", "industria", "corporativo", "franquia", "grande porte", "gestão empresarial"));
        boolean irrelevantUtilityRisk = irrelevantUtilityRisk(url, combined);
        int qualityScore = routineEvidenceScore + brazilRelevanceScore - (solutionLanguageRisk ? 35 : 0) - (commercialPageRisk ? 20 : 0) - (structuredBusinessDriftRisk ? 15 : 0) - (irrelevantUtilityRisk ? 100 : 0);
        String sourceIntent = sourceIntent(solutionLanguageRisk, commercialPageRisk, irrelevantUtilityRisk, combined);

        qualified.put("url", url);
        qualified.put("title", title);
        qualified.put("snippet", snippet);
        qualified.put("semanticEvidenceClassification", sourceIntent);
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
        qualified.put("irrelevantUtilityRisk", irrelevantUtilityRisk);
        qualified.put("qualityScore", Math.max(0, qualityScore));
        qualified.putIfAbsent("matchedQuery", text(plannedQuery.get("query")));
        qualified.putIfAbsent("queryIntent", text(plannedQuery.get("intent")));
        qualified.putIfAbsent("routineRelevance", text(plannedQuery.get("objective")));
        return qualified;
    }

    /** Decide se a fonte pode avançar para coleta de snapshot da rotina. */
    private boolean isQualifiedRoutineSource(Map<String, Object> source) {
        return List.of("ROUTINE_EVIDENCE", "COMMUNITY_OR_QUESTION_EVIDENCE").contains(source.get("sourceIntent"))
                && score(source, "routineEvidenceScore") >= MIN_ROUTINE_EVIDENCE_SCORE
                && score(source, "qualityScore") >= MIN_ROUTINE_EVIDENCE_SCORE;
    }

    /** Gera variações curtas de busca para evitar que queries longas impeçam fontes úteis. */
    private List<String> queryVariants(Map<String, Object> plannedQuery) {
        String originalQuery = text(plannedQuery.get("query"));
        String objective = text(plannedQuery.get("objective"));
        String simplifiedQuery = simplifyQuery(originalQuery);
        String operationalQuery = operationalQuery(simplifiedQuery);
        List<String> variants = new ArrayList<>();
        addVariant(variants, enrichBrazilFirstQuery(simplifiedQuery));
        addVariant(variants, enrichBrazilFirstQuery(operationalQuery));
        addVariant(variants, realPainQuery(operationalQuery, simplifiedQuery));
        addVariant(variants, complaintQuery(operationalQuery, simplifiedQuery));
        addVariant(variants, professionalQuestionQuery(operationalQuery, simplifiedQuery));
        addVariant(variants, enrichBrazilFirstQuery(objective));
        addVariant(variants, naturalRoutineQuery(simplifiedQuery));
        return variants;
    }

    /** Enriquece a consulta para priorizar Brasil e rotina real do profissional. */
    private String enrichBrazilFirstQuery(String query) {
        String cleanQuery = trimWords(query, 12);
        if (cleanQuery.isBlank()) {
            return "";
        }
        return cleanQuery + " Brasil rotina cliente atendimento cobrança";
    }

    /** Cria variação semântica ampla para encontrar relatos e dúvidas reais. */
    private String naturalRoutineQuery(String query) {
        String cleanQuery = trimWords(query, 10);
        return cleanQuery.isBlank() ? "" : "como é a rotina " + cleanQuery + " problemas clientes whatsapp instagram";
    }

    /** Cria variação focada em dor real, evitando que a busca fique presa em definições genéricas de rotina. */
    private String realPainQuery(String operationalQuery, String fallbackQuery) {
        String base = trimWords(operationalQuery.isBlank() ? fallbackQuery : operationalQuery, 8);
        return base.isBlank() ? "" : base + " problema cliente pedido troca entrega whatsapp Brasil";
    }

    /** Cria variação em fontes de reclamação pública para capturar atritos concretos de compra/atendimento. */
    private String complaintQuery(String operationalQuery, String fallbackQuery) {
        String base = trimWords(operationalQuery.isBlank() ? fallbackQuery : operationalQuery, 7);
        return base.isBlank() ? "" : "site:reclameaqui.com.br " + base + " cliente troca entrega pedido loja roupa";
    }

    /** Cria variação em formato de pergunta para encontrar dúvidas operacionais de profissionais e clientes. */
    private String professionalQuestionQuery(String operationalQuery, String fallbackQuery) {
        String base = trimWords(operationalQuery.isBlank() ? fallbackQuery : operationalQuery, 7);
        return base.isBlank() ? "" : "\"" + base + "\" dúvida cliente estoque atendimento whatsapp";
    }

    /** Simplifica a query planejada removendo ruído que reduz a chance de resultado rastreável. */
    private String simplifyQuery(String query) {
        String withoutParenthetical = PARENTHETICAL_TEXT.matcher(query).replaceAll(" ");
        String searchable = NON_SEARCH_TEXT.matcher(withoutParenthetical).replaceAll(" ");
        return MULTIPLE_SPACES.matcher(searchable).replaceAll(" ").trim();
    }

    /** Mantém termos de operação diária mais fortes para uma variação de busca objetiva. */
    private String operationalQuery(String query) {
        String normalized = normalize(query);
        List<String> terms = List.of("atendimento", "provador", "estoque", "reposicao", "reposição", "mercadoria", "vitrine", "araras", "etiquetas", "caixa", "loja", "roupas", "acessorios", "acessórios", "pagamentos", "comprovantes", "pedidos", "reservas", "trocas", "devolucoes", "devoluções", "entregas", "whatsapp", "instagram");
        StringBuilder builder = new StringBuilder();
        for (String term : terms) {
            if (normalized.contains(normalize(term)) && !builder.toString().contains(term)) {
                builder.append(term).append(' ');
            }
        }
        String operational = builder.toString().trim();
        return operational.isBlank() ? query : operational;
    }

    /** Adiciona uma variação não vazia sem duplicidade. */
    private void addVariant(List<String> variants, String query) {
        if (!query.isBlank() && !variants.contains(query)) {
            variants.add(query);
        }
    }

    /** Limita o texto aos primeiros termos relevantes para busca pública. */
    private String trimWords(String query, int maxWords) {
        String[] words = MULTIPLE_SPACES.matcher(query).replaceAll(" ").trim().split(" ");
        if (words.length <= maxWords) {
            return query.trim();
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < maxWords; index++) {
            builder.append(words[index]).append(' ');
        }
        return builder.toString().trim();
    }

    /** Explica por que uma fonte foi recusada, preservando auditoria da decisão. */
    private String rejectionReason(Map<String, Object> source, Set<String> seenUrls) {
        String url = text(source.get("url"));
        if (url.isBlank()) {
            return "URL_AUSENTE";
        }
        if (seenUrls.contains(url)) {
            return "URL_DUPLICADA";
        }
        if (!List.of("ROUTINE_EVIDENCE", "COMMUNITY_OR_QUESTION_EVIDENCE").contains(source.get("sourceIntent"))) {
            return "RISCO_CONTAMINACAO_SOLUCAO_OU_COMERCIAL";
        }
        if (score(source, "routineEvidenceScore") < MIN_ROUTINE_EVIDENCE_SCORE) {
            return "EVIDENCIA_ROTINA_INSUFICIENTE";
        }
        if (score(source, "qualityScore") < MIN_ROUTINE_EVIDENCE_SCORE) {
            return "QUALIDADE_INSUFICIENTE";
        }
        return "";
    }

    /** Calcula score de evidência de rotina operacional. */
    private int routineEvidenceScore(String text) {
        return scoreByTerms(text, List.of("rotina", "tarefa", "tarefas", "atendimento", "agenda", "cliente", "clientes", "cobrança", "cobranca", "preço", "preco", "orçamento", "orcamento", "material", "estoque", "retrabalho", "problema", "dúvida", "duvida", "pergunta", "frequência", "frequencia", "manual", "whatsapp", "instagram", "indicação", "indicacao", "balcão", "balcao", "delivery", "relato", "pedido", "pedidos", "troca", "trocas", "devolução", "devolucao", "entrega", "entregas", "reserva", "reservas", "reclamação", "reclamacao", "reclame aqui"), 10);
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

    /** Classifica semanticamente a fonte separando evidência real de contaminação comercial. */
    private String sourceIntent(boolean solutionLanguageRisk, boolean commercialPageRisk, boolean irrelevantUtilityRisk, String text) {
        if (irrelevantUtilityRisk) {
            return "IRRELEVANT_UTILITY_RISK";
        }
        if (solutionLanguageRisk || commercialPageRisk) {
            return "CONTAMINATION_RISK";
        }
        if (containsAny(text, List.of("pergunta", "dúvida", "duvida", "relato", "comentário", "comentario", "forum", "fórum", "reclamação", "reclamacao", "reclame aqui"))) {
            return "COMMUNITY_OR_QUESTION_EVIDENCE";
        }
        if (containsAny(text, List.of("whatsapp", "instagram", "agenda", "cobrança", "cobranca", "cliente", "atendimento", "estoque", "balcão", "balcao", "pedido", "pedidos", "troca", "trocas", "entrega", "entregas", "reserva", "reservas"))) {
            return "ROUTINE_EVIDENCE";
        }
        return "GENERIC_SECTOR_SOURCE";
    }

    /** Identifica resultados utilitários ou dicionários fora da rotina real do CNAE. */
    private boolean irrelevantUtilityRisk(String url, String text) {
        String domain = domain(url);
        return containsAny(domain + " " + text, List.of("calculator", "calculadora", "dicionario", "dicionário", "sinonimos", "sinônimos", "infopedia", "lexico", "desmos", "app store", "dicio.com.br", "michaelis", "conceito.de", "significados", "aulete"));
    }

    /** Classifica o tipo operacional da fonte. */
    private String sourceType(String url, String text, String sourceIntent) {
        if ("CONTAMINATION_RISK".equals(sourceIntent)) {
            return "COMMERCIAL_OR_SOLUTION_RISK";
        }
        if ("COMMUNITY_OR_QUESTION_EVIDENCE".equals(sourceIntent)) {
            return "REAL_PROFESSIONAL_QUESTION";
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

    /** Normaliza acentos para comparar termos de busca de forma estável. */
    private String normalize(String value) {
        return Normalizer.normalize(text(value).toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
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

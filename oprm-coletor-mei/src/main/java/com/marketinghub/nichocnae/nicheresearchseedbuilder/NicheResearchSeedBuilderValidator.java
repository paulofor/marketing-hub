package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Valida regras determinísticas da etapa dois antes de enviar o seed e as queries ao backend. */
@Component
public class NicheResearchSeedBuilderValidator {
    private static final int MIN_QUERIES = 12;
    private static final int MAX_QUERIES = 15;
    private static final Set<String> ALLOWED_GOALS = Set.of(
            "ROUTINE_DISCOVERY",
            "ROUTINE_TASK_DISCOVERY",
            "OPERATIONAL_DIFFICULTY_DISCOVERY",
            "NICHE_OWNER_QUESTION_DISCOVERY",
            "FINAL_CUSTOMER_QUESTION_DISCOVERY",
            "LANGUAGE_DISCOVERY",
            "OPERATIONAL_CONTEXT_DISCOVERY");
    private static final Set<String> SOLUTION_TERMS = Set.of(
            "ia",
            "inteligencia artificial",
            "automacao",
            "software",
            "sistema",
            "app",
            "ferramenta",
            "curso",
            "template",
            "oferta",
            "landing page");

    /** Garante que o modelo produziu seed completo e queries específicas, pendentes e sem contaminação de solução. */
    public void validate(NicheResearchSeedBuilderPending input, NicheResearchSeedBuilderOutput output) {
        if (output == null) {
            throw new IllegalArgumentException("Saída da etapa dois não pode ser nula.");
        }
        if (!input.researchCycleId().equals(output.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId da saída não corresponde ao ciclo processado.");
        }
        validateSeed(input, output.seed());
        validateQueries(output.seed(), output.queries());
    }

    /** Valida os campos essenciais do seed que identificam o nicho operacional pesquisado. */
    private void validateSeed(NicheResearchSeedBuilderPending input, NicheResearchSeed seed) {
        if (seed == null) {
            throw new IllegalArgumentException("Seed da etapa dois não pode ser nulo.");
        }
        if (!input.researchCycleId().equals(seed.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId do seed não corresponde ao ciclo processado.");
        }
        requireText(seed.nicheName(), "nicheName");
        requireText(seed.businessType(), "businessType");
        requireText(seed.operationType(), "operationType");
        requireText(seed.customerType(), "customerType");
        requireText(seed.commercialObjects(), "commercialObjects");
        requireText(seed.initialAssumptions(), "initialAssumptions");
        if (!"AI".equals(seed.createdBy())) {
            throw new IllegalArgumentException("createdBy do seed deve ser AI.");
        }
    }

    /** Valida quantidade, status, objetivo, especificidade e ausência de linguagem de solução nas queries. */
    private void validateQueries(NicheResearchSeed seed, List<ResearchQuery> queries) {
        if (queries == null || queries.size() < MIN_QUERIES || queries.size() > MAX_QUERIES) {
            throw new IllegalArgumentException("A etapa dois deve gerar entre 12 e 15 queries.");
        }

        Set<String> uniqueTexts = new HashSet<>();
        Set<String> anchors = buildAnchors(seed);
        String allowedCnaeText = normalizeForTerms(seed.cnaeDescription());
        for (ResearchQuery query : queries) {
            validateQuery(seed, query, uniqueTexts, anchors, allowedCnaeText);
        }
    }

    /** Valida uma query individual antes de ela ser persistida como linha própria no backend. */
    private void validateQuery(
            NicheResearchSeed seed,
            ResearchQuery query,
            Set<String> uniqueTexts,
            Set<String> anchors,
            String allowedCnaeText) {
        if (query == null) {
            throw new IllegalArgumentException("Query da etapa dois não pode ser nula.");
        }
        if (!seed.researchCycleId().equals(query.researchCycleId())) {
            throw new IllegalArgumentException("researchCycleId da query não corresponde ao seed.");
        }
        requireText(query.queryText(), "queryText");
        String normalized = normalize(query.queryText());
        if (!uniqueTexts.add(normalized)) {
            throw new IllegalArgumentException("Query duplicada na etapa dois: " + query.queryText());
        }
        if ("como vender mais".equals(normalized)) {
            throw new IllegalArgumentException("Query genérica proibida: " + query.queryText());
        }
        if (anchors.stream().noneMatch(normalized::contains)) {
            throw new IllegalArgumentException("Query sem nicho ou contexto operacional específico: " + query.queryText());
        }
        rejectSolutionTerms(query.queryText(), allowedCnaeText);
        if (!ALLOWED_GOALS.contains(query.queryGoal())) {
            throw new IllegalArgumentException("queryGoal inválido na etapa dois: " + query.queryGoal());
        }
        if (!"PENDING".equals(query.status())) {
            throw new IllegalArgumentException("Query da etapa dois deve iniciar com status PENDING.");
        }
        if (!"AI".equals(query.createdBy())) {
            throw new IllegalArgumentException("Query da etapa dois deve iniciar com createdBy AI.");
        }
    }

    /** Rejeita linguagem de solução que não aparece literalmente na descrição CNAE do ciclo. */
    private void rejectSolutionTerms(String queryText, String allowedCnaeText) {
        String normalizedQuery = normalizeForTerms(queryText);
        Set<String> queryTokens = Set.of(normalizedQuery.split("[^a-z0-9]+"));
        for (String term : SOLUTION_TERMS) {
            if (containsTerm(normalizedQuery, queryTokens, term) && !containsAllowedCnaeTerm(allowedCnaeText, term)) {
                throw new IllegalArgumentException("Query com linguagem de solução proibida na etapa dois: " + queryText);
            }
        }
    }

    /** Verifica se a descrição CNAE contém literalmente o termo sensível e autoriza sua presença na query. */
    private boolean containsAllowedCnaeTerm(String allowedCnaeText, String term) {
        Set<String> allowedTokens = Set.of(allowedCnaeText.split("[^a-z0-9]+"));
        return containsTerm(allowedCnaeText, allowedTokens, term);
    }

    /** Detecta termos simples por token e expressões compostas por ocorrência textual normalizada. */
    private boolean containsTerm(String text, Set<String> tokens, String term) {
        return term.contains(" ") ? text.contains(term) : tokens.contains(term);
    }

    /** Extrai termos âncora do nicho, descrição CNAE e objetos operacionais para bloquear queries genéricas. */
    private Set<String> buildAnchors(NicheResearchSeed seed) {
        Set<String> anchors = new HashSet<>();
        addWords(anchors, seed.nicheName());
        addWords(anchors, seed.cnaeDescription());
        addWords(anchors, seed.businessType());
        addWords(anchors, seed.operationType());
        addWords(anchors, seed.customerType());
        addWords(anchors, seed.commercialObjects());
        return anchors;
    }

    /** Adiciona palavras relevantes como âncoras de especificidade das queries. */
    private void addWords(Set<String> anchors, String value) {
        if (value == null) {
            return;
        }
        String normalized = normalize(value);
        for (String rawExpression : normalized.split(",")) {
            String expression = rawExpression.trim();
            if (!expression.isBlank()) {
                anchors.add(expression);
            }
        }
        for (String rawWord : normalized.split("[^a-z0-9áàâãéêíóôõúç]+")) {
            if (rawWord.length() >= 5) {
                anchors.add(rawWord);
            }
        }
    }

    /** Exige texto funcional preenchido para campos obrigatórios da etapa dois. */
    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório vazio na etapa dois: " + fieldName);
        }
    }

    /** Normaliza texto para comparação simples de duplicidade e especificidade. */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Normaliza texto removendo acentos para comparar termos de solução com baixa ambiguidade. */
    private String normalizeForTerms(String value) {
        String normalized = Normalizer.normalize(normalize(value), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}

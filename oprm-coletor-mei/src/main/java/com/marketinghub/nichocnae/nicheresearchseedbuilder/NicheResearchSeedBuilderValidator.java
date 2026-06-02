package com.marketinghub.nichocnae.nicheresearchseedbuilder;

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
            "NICHE_OWNER_QUESTION_DISCOVERY",
            "FINAL_CUSTOMER_QUESTION_DISCOVERY",
            "SALES_PAIN_DISCOVERY",
            "PRODUCT_SERVICE_DISCOVERY",
            "OFFER_PATTERN_DISCOVERY");

    /** Garante que o modelo produziu seed completo e queries específicas, pendentes e sem duplicidade. */
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

    /** Valida quantidade, status, objetivo e especificidade básica das queries de pesquisa. */
    private void validateQueries(NicheResearchSeed seed, List<ResearchQuery> queries) {
        if (queries == null || queries.size() < MIN_QUERIES || queries.size() > MAX_QUERIES) {
            throw new IllegalArgumentException("A etapa dois deve gerar entre 12 e 15 queries.");
        }

        Set<String> uniqueTexts = new HashSet<>();
        Set<String> anchors = buildAnchors(seed);
        for (ResearchQuery query : queries) {
            validateQuery(seed, query, uniqueTexts, anchors);
        }
    }

    /** Valida uma query individual antes de ela ser persistida como linha própria no backend. */
    private void validateQuery(
            NicheResearchSeed seed,
            ResearchQuery query,
            Set<String> uniqueTexts,
            Set<String> anchors) {
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
            throw new IllegalArgumentException("Query sem nicho ou objeto comercial específico: " + query.queryText());
        }
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

    /** Extrai termos âncora do nicho e objetos comerciais para bloquear queries genéricas. */
    private Set<String> buildAnchors(NicheResearchSeed seed) {
        Set<String> anchors = new HashSet<>();
        addWords(anchors, seed.nicheName());
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
}

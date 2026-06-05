package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Classifica resultados públicos por aderência à rotina real antes da persistência da etapa três. */
@Component
public class SourceIntentClassifier {
    private static final String INTENT_ROUTINE_REPORT = "ROUTINE_REPORT";
    private static final String INTENT_REAL_QUESTION = "REAL_QUESTION";
    private static final String INTENT_PRACTICAL_GUIDE = "PRACTICAL_GUIDE";
    private static final String INTENT_EDUCATIONAL_CONTENT = "EDUCATIONAL_CONTENT";
    private static final String INTENT_COMMERCIAL_PAGE_RISK = "COMMERCIAL_PAGE_RISK";
    private static final String INTENT_GENERIC_PUBLIC_CONTENT = "GENERIC_PUBLIC_CONTENT";
    private static final List<String> ROUTINE_TERMS = List.of(
            "rotina", "dia a dia", "tarefas", "atividade", "trabalho", "processo", "procedimento", "operação");
    private static final List<String> PROBLEM_TERMS = List.of(
            "problema", "dificuldade", "desafio", "erro", "reclamação", "dúvida", "pergunta", "como fazer");
    private static final List<String> GUIDE_TERMS = List.of(
            "guia", "passo a passo", "manual", "tutorial", "boas práticas", "checklist", "orientação");
    private static final List<String> EDUCATIONAL_TERMS = List.of(
            "curso", "aula", "apostila", "artigo", "entenda", "o que é", "conceito");
    private static final List<String> COMMERCIAL_TERMS = List.of(
            "comprar", "preço", "orçamento", "promoção", "software", "plataforma", "ferramenta", "curso online",
            "consultoria", "agende", "contrate", "venda", "solução", "produto", "serviço");
    private static final List<String> COMMERCIAL_DOMAINS = List.of(
            "hotmart.", "kiwify.", "eduzz.", "monetizze.", "shopify.", "mercadolivre.", "amazon.");

    /** Retorna uma cópia do resultado com intenção, escore e riscos calculados por heurística conservadora. */
    public SourceSearchResult classify(SourceSearchResult result) {
        String text = normalized(result.sourceTitle() + " " + result.sourceSnippet() + " " + result.sourceUrl());
        String domain = normalized(result.sourceDomain());
        int routineHits = countHits(text, ROUTINE_TERMS);
        int problemHits = countHits(text, PROBLEM_TERMS);
        int guideHits = countHits(text, GUIDE_TERMS);
        int educationalHits = countHits(text, EDUCATIONAL_TERMS);
        int commercialHits = countHits(text, COMMERCIAL_TERMS) + countHits(domain, COMMERCIAL_DOMAINS);
        boolean commercialRisk = commercialHits > 0 && routineHits + problemHits < 2;
        boolean solutionRisk =
                containsAny(text, List.of("solução", "software", "ferramenta", "produto", "venda", "comprar"));
        String intent = classifyIntent(routineHits, problemHits, guideHits, educationalHits, commercialRisk);
        int score = routineEvidenceScore(routineHits, problemHits, guideHits, educationalHits, commercialHits);
        return new SourceSearchResult(
                result.sourceUrl(),
                result.sourceTitle(),
                result.sourceSnippet(),
                result.sourceDomain(),
                result.searchPosition(),
                intent,
                score,
                commercialRisk,
                solutionRisk);
    }

    /** Decide a intenção operacional priorizando rotina, perguntas reais e guias não vendedores. */
    private String classifyIntent(
            int routineHits, int problemHits, int guideHits, int educationalHits, boolean commercialRisk) {
        if (commercialRisk) {
            return INTENT_COMMERCIAL_PAGE_RISK;
        }
        if (routineHits > 0) {
            return INTENT_ROUTINE_REPORT;
        }
        if (problemHits > 0) {
            return INTENT_REAL_QUESTION;
        }
        if (guideHits > 0) {
            return INTENT_PRACTICAL_GUIDE;
        }
        if (educationalHits > 0) {
            return INTENT_EDUCATIONAL_CONTENT;
        }
        return INTENT_GENERIC_PUBLIC_CONTENT;
    }

    /** Calcula escore simples para ordenar fontes com maior evidência de rotina antes de páginas comerciais. */
    private int routineEvidenceScore(
            int routineHits, int problemHits, int guideHits, int educationalHits, int commercialHits) {
        int score = 45
                + routineHits * 20
                + problemHits * 15
                + guideHits * 10
                + educationalHits * 5
                - commercialHits * 20;
        return Math.max(0, Math.min(100, score));
    }

    /** Conta quantos grupos de termos aparecem no texto normalizado. */
    private int countHits(String text, List<String> terms) {
        int hits = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                hits++;
            }
        }
        return hits;
    }

    /** Indica se pelo menos um termo aparece no texto normalizado. */
    private boolean containsAny(String text, List<String> terms) {
        return countHits(text, terms) > 0;
    }

    /** Normaliza texto para comparação determinística sem depender de IA. */
    private String normalized(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT) : "";
    }
}

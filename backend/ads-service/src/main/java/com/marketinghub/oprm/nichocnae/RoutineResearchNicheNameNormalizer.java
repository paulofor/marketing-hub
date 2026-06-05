package com.marketinghub.oprm.nichocnae;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normaliza nomes de nicho CNAE para impedir que linguagem de solução direcione a pesquisa de rotina.
 */
public class RoutineResearchNicheNameNormalizer {
    private static final BigDecimal LOW_RISK_SCORE = BigDecimal.ZERO;
    private static final BigDecimal HIGH_RISK_SCORE = new BigDecimal("100.00");
    private static final int MINIMUM_STRONG_NAME_LENGTH = 3;
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s+");
    private static final Pattern SEPARATOR_PREFIX = Pattern.compile("^[\\s:;,.\\-–—]+|[\\s:;,.\\-–—]+$");
    private static final List<Pattern> SOLUTION_PREFIXES = List.of(
            caseInsensitivePrefix("IA\\s+para\\s+crescimento\\s+de"),
            caseInsensitivePrefix("intelig[eê]ncia\\s+artificial\\s+para\\s+crescimento\\s+de"),
            caseInsensitivePrefix("automa[cç][aã]o\\s+para"),
            caseInsensitivePrefix("sistema\\s+para"),
            caseInsensitivePrefix("app\\s+para"),
            caseInsensitivePrefix("software\\s+para"),
            caseInsensitivePrefix("curso\\s+para"),
            caseInsensitivePrefix("ferramenta\\s+para"),
            caseInsensitivePrefix("marketing\\s+digital\\s+para"));

    /** Normaliza o nome original e calcula o risco de contaminação por linguagem de solução. */
    public NormalizedNicheName normalize(String originalNicheName, String cnaeDescription) {
        String original = clean(originalNicheName);
        String normalized = original;
        boolean solutionLanguageDetected = false;
        for (Pattern solutionPrefix : SOLUTION_PREFIXES) {
            String candidate = solutionPrefix.matcher(normalized).replaceFirst("");
            if (!candidate.equals(normalized)) {
                solutionLanguageDetected = true;
                normalized = clean(candidate);
                break;
            }
        }
        String neutralName = isWeak(normalized) ? clean(cnaeDescription) : normalized;
        if (isWeak(neutralName)) {
            neutralName = original;
        }
        return new NormalizedNicheName(original, neutralName, riskScore(solutionLanguageDetected));
    }

    /** Limpa espaços e separadores excedentes preservando a linguagem funcional do nicho. */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        String withoutExtraSpaces = EXTRA_SPACES.matcher(value.trim()).replaceAll(" ");
        return SEPARATOR_PREFIX.matcher(withoutExtraSpaces).replaceAll("");
    }

    /** Indica se o nome operacional ficou fraco demais para guiar a pesquisa de rotina. */
    private boolean isWeak(String value) {
        return value == null || value.length() < MINIMUM_STRONG_NAME_LENGTH;
    }

    /** Calcula o score simples de risco para auditoria operacional do ciclo. */
    private BigDecimal riskScore(boolean solutionLanguageDetected) {
        return solutionLanguageDetected ? HIGH_RISK_SCORE : LOW_RISK_SCORE;
    }

    /** Cria um padrão de prefixo tolerante a maiúsculas/minúsculas e acentuação nos termos cadastrados. */
    private static Pattern caseInsensitivePrefix(String expression) {
        return Pattern.compile("^\\s*(?:" + expression + ")\\b\\s*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Resultado estruturado da normalização do nome usado para abrir o ciclo de pesquisa. */
    public record NormalizedNicheName(String originalNicheName, String neutralNicheName, BigDecimal solutionLanguageRiskScore) {}
}

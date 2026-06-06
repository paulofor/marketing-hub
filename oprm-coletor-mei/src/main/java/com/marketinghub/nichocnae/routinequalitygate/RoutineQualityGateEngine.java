package com.marketinghub.nichocnae.routinequalitygate;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Avalia deterministicamente se um cartão representa bem a rotina real e suas dificuldades observáveis. */
@Component
public class RoutineQualityGateEngine {
    private static final String LIGHTLY_RESEARCHED = "LIGHTLY_RESEARCHED";
    private static final String NEEDS_MORE_RESEARCH = "NEEDS_MORE_RESEARCH";
    private static final String GENERIC = "GENERIC";
    private static final int MAX_ACCEPTABLE_SOLUTION_RISK = 35;

    /** Calcula a decisão da etapa sete sem exigir sinal de solução, hipótese, mecanismo ou oportunidade comercial. */
    public RoutineQualityDecision evaluate(RoutineQualityGatePending pending) {
        int sourceCount = value(pending.sourceCount());
        int signalCount = value(pending.signalCount());
        int solutionRiskScore = calculateSolutionLanguageRiskScore(pending);
        int textualSolutionRiskScore = calculateTextualSolutionLanguageRiskScore(pending);
        int effectiveSolutionRiskScore = Math.max(solutionRiskScore, textualSolutionRiskScore);
        int specificityScore = calculateSpecificityScore(pending);
        int confidenceScore = calculateConfidenceScore(pending, sourceCount, signalCount, effectiveSolutionRiskScore);
        int duplicationScore = calculateDuplicationScore(pending);
        boolean hasRequiredSummaries = hasText(pending.routineSummary()) && hasText(pending.painsSummary());
        boolean hasAuditableEvidence = hasText(pending.evidenceSummary()) && distinctDomainCount(pending.sourceDomains()) >= 2;
        boolean hasRoutineTask = value(pending.routineTaskCount()) > 0;
        boolean hasDifficulty = value(pending.operationalDifficultyCount()) > 0 || value(pending.painSignalCount()) > 0;
        boolean hasQuestionOrLanguage = value(pending.questionSignalCount()) > 0 || value(pending.languageMarkerCount()) > 0;
        boolean hasMinimumSignalMix = hasRoutineTask && hasDifficulty && hasQuestionOrLanguage;
        boolean dominatedBySolution = effectiveSolutionRiskScore > MAX_ACCEPTABLE_SOLUTION_RISK
                || value(pending.solutionLanguageRiskCount()) > signalCount / 2;
        boolean generic = specificityScore < 40 || duplicationScore >= 70 || !hasRequiredSummaries || !hasAuditableEvidence;
        boolean approved = sourceCount >= 3
                && signalCount >= 6
                && specificityScore >= 60
                && confidenceScore >= 50
                && hasRequiredSummaries
                && hasAuditableEvidence
                && hasMinimumSignalMix
                && !dominatedBySolution
                && !generic;
        String status = generic ? GENERIC : approved ? LIGHTLY_RESEARCHED : NEEDS_MORE_RESEARCH;
        return new RoutineQualityDecision(
                status,
                approved,
                specificityScore,
                confidenceScore,
                duplicationScore,
                buildNotes(
                        pending,
                        status,
                        hasMinimumSignalMix,
                        hasAuditableEvidence,
                        dominatedBySolution,
                        effectiveSolutionRiskScore,
                        textualSolutionRiskScore));
    }

    /** Calcula especificidade combinando rotina, dificuldade, perguntas/linguagem e variedade de fontes. */
    private int calculateSpecificityScore(RoutineQualityGatePending pending) {
        int score = 0;
        score += cappedLengthScore(pending.routineSummary(), 18);
        score += cappedLengthScore(pending.painsSummary(), 18);
        score += cappedLengthScore(pending.resultsSummary(), 12);
        score += Math.min(18, value(pending.sourceDiversityScore()) / 4 + distinctDomainCount(pending.sourceDomains()) * 3);
        score += Math.min(24, value(pending.routineTaskCount()) * 5
                + (value(pending.operationalDifficultyCount()) + value(pending.painSignalCount())) * 4
                + (value(pending.questionSignalCount()) + value(pending.languageMarkerCount())) * 2);
        if (containsConcreteMarker(pending.routineSummary() + " " + pending.painsSummary() + " " + pending.resultsSummary())) {
            score += 8;
        }
        return clamp(score);
    }

    /** Calcula confiança efetiva com bônus para evidência de rotina/dificuldade e penalidade por solução precoce. */
    private int calculateConfidenceScore(
            RoutineQualityGatePending pending, int sourceCount, int signalCount, int solutionRiskScore) {
        int base = value(pending.cardConfidenceScore());
        int sourceScore = Math.min(22, sourceCount * 5);
        int signalScore = Math.min(20, signalCount * 2);
        int evidenceScore = Math.min(28, (value(pending.routineEvidenceScore()) + value(pending.difficultyEvidenceScore())) / 7);
        int riskPenalty = Math.min(35, solutionRiskScore / 2);
        return clamp(Math.round((base * 0.35f) + sourceScore + signalScore + evidenceScore - riskPenalty));
    }

    /** Calcula risco de duplicação/genérico por repetição simples entre os blocos principais. */
    private int calculateDuplicationScore(RoutineQualityGatePending pending) {
        List<String> normalized = List.of(
                normalize(pending.routineSummary()),
                normalize(pending.painsSummary()),
                normalize(pending.resultsSummary()),
                normalize(pending.mechanismOpportunitiesSummary()));
        int duplicatePairs = 0;
        for (int i = 0; i < normalized.size(); i++) {
            for (int j = i + 1; j < normalized.size(); j++) {
                if (StringUtils.hasText(normalized.get(i)) && normalized.get(i).equals(normalized.get(j))) {
                    duplicatePairs++;
                }
            }
        }
        int score = duplicatePairs * 25;
        if (normalized.stream().filter(StringUtils::hasText).distinct().count() <= 2) {
            score += 25;
        }
        return clamp(score);
    }

    /** Calcula o risco consolidado de linguagem de solução vindo do backend e dos contadores de sinais. */
    private int calculateSolutionLanguageRiskScore(RoutineQualityGatePending pending) {
        int requestScore = value(pending.solutionLanguageRiskScore());
        int signalCount = Math.max(1, value(pending.signalCount()));
        int counterScore = clamp((int) Math.round((value(pending.solutionLanguageRiskCount()) * 100.0) / signalCount));
        return Math.max(requestScore, counterScore);
    }

    /** Calcula risco textual quando o card usa termos de solução mesmo sem contador de risco persistido. */
    private int calculateTextualSolutionLanguageRiskScore(RoutineQualityGatePending pending) {
        String text = normalize(String.join(
                " ",
                nullToEmpty(pending.routineSummary()),
                nullToEmpty(pending.painsSummary()),
                nullToEmpty(pending.resultsSummary()),
                nullToEmpty(pending.mechanismOpportunitiesSummary())));
        int matches = 0;
        for (String term : List.of(
                " ia ",
                " inteligencia artificial ",
                " automacao ",
                " software ",
                " sistema ",
                " app ",
                " ferramenta ",
                " curso ",
                " template ",
                " oferta ",
                " landing page ")) {
            if ((" " + text + " ").contains(term)) {
                matches++;
            }
        }
        return clamp(matches * 12);
    }

    /** Monta notas objetivas para explicar a decisão operacional do gate. */
    private String buildNotes(
            RoutineQualityGatePending pending,
            String status,
            boolean hasMinimumSignalMix,
            boolean hasAuditableEvidence,
            boolean dominatedBySolution,
            int solutionRiskScore,
            int textualSolutionRiskScore) {
        List<String> notes = new ArrayList<>();
        notes.add("status=" + status);
        notes.add("fontes=" + value(pending.sourceCount()));
        notes.add("sinais=" + value(pending.signalCount()));
        notes.add("tarefasRotina=" + value(pending.routineTaskCount()));
        notes.add("dificuldades=" + (value(pending.operationalDifficultyCount()) + value(pending.painSignalCount())));
        notes.add("perguntasOuLinguagem=" + (value(pending.questionSignalCount()) + value(pending.languageMarkerCount())));
        notes.add("mixMinimoRotina=" + hasMinimumSignalMix);
        notes.add("evidenciaAuditavel=" + hasAuditableEvidence);
        notes.add("riscoLinguagemSolucao=" + solutionRiskScore);
        notes.add("riscoTextualSolucao=" + textualSolutionRiskScore);
        notes.add("dominadoPorSolucao=" + dominatedBySolution);
        notes.add("fontesDistintas=" + distinctDomainCount(pending.sourceDomains()));
        return String.join("; ", notes);
    }

    /** Converte texto em pontuação por tamanho útil com teto informado. */
    private int cappedLengthScore(String value, int cap) {
        if (!hasText(value)) {
            return 0;
        }
        return Math.min(cap, value.trim().length() / 30);
    }

    /** Conta domínios distintos declarados na síntese para estimar variedade de fontes. */
    private int distinctDomainCount(String sourceDomains) {
        if (!hasText(sourceDomains)) {
            return 0;
        }
        return (int) List.of(sourceDomains.split(",")).stream().map(String::trim).filter(StringUtils::hasText).distinct().count();
    }

    /** Detecta marcadores concretos que normalmente indicam texto menos genérico. */
    private boolean containsConcreteMarker(String value) {
        return hasText(value) && value.matches(".*(\\d|WhatsApp|Instagram|agenda|cliente|preço|pacote|retorno|horário|cancelamento|atraso|falta).*");
    }

    /** Normaliza texto para comparação de duplicidade simples. */
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase()
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('ã', 'a')
                .replace('â', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('õ', 'o')
                .replace('ô', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Retorna texto vazio para campos opcionais ausentes antes de unir os blocos avaliados. */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Verifica se existe texto útil no valor recebido. */
    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /** Converte nulo para zero para cálculos determinísticos. */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /** Garante que a pontuação calculada permaneça na escala percentual. */
    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}

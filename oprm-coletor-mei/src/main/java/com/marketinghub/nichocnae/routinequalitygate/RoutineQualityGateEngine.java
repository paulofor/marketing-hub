package com.marketinghub.nichocnae.routinequalitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Avalia deterministicamente se um cartão de rotina está pronto para alimentar hipóteses comerciais. */
@Component
public class RoutineQualityGateEngine {
    private static final String LIGHTLY_RESEARCHED = "LIGHTLY_RESEARCHED";
    private static final String NEEDS_MORE_RESEARCH = "NEEDS_MORE_RESEARCH";
    private static final String GENERIC = "GENERIC";

    /** Calcula a decisão da etapa sete com base em fontes, sinais e especificidade textual do cartão. */
    public RoutineQualityDecision evaluate(RoutineQualityGatePending pending) {
        int sourceCount = value(pending.sourceCount());
        int signalCount = value(pending.signalCount());
        int specificityScore = calculateSpecificityScore(pending);
        int confidenceScore = calculateConfidenceScore(pending, sourceCount, signalCount);
        int duplicationScore = calculateDuplicationScore(pending);
        boolean hasRequiredSummaries = hasText(pending.routineSummary())
                && hasText(pending.painsSummary())
                && hasText(pending.mechanismOpportunitiesSummary());
        boolean hasMinimumSignalMix = value(pending.questionSignalCount()) > 0
                && value(pending.painSignalCount()) > 0
                && value(pending.mechanismOpportunityCount()) > 0
                && value(pending.routineTaskCount()) > 0;
        boolean generic = specificityScore < 40 || duplicationScore >= 70 || !hasRequiredSummaries;
        boolean approved = sourceCount >= 5
                && signalCount >= 10
                && specificityScore >= 60
                && confidenceScore >= 50
                && hasRequiredSummaries
                && hasMinimumSignalMix
                && !generic;
        String status = generic ? GENERIC : approved ? LIGHTLY_RESEARCHED : NEEDS_MORE_RESEARCH;
        return new RoutineQualityDecision(status, approved, specificityScore, confidenceScore, duplicationScore, buildNotes(pending, status, hasMinimumSignalMix));
    }

    /** Calcula especificidade combinando tamanho útil, presença de números/domínios e variedade de sinais. */
    private int calculateSpecificityScore(RoutineQualityGatePending pending) {
        int score = 0;
        score += cappedLengthScore(pending.routineSummary(), 18);
        score += cappedLengthScore(pending.painsSummary(), 16);
        score += cappedLengthScore(pending.mechanismOpportunitiesSummary(), 16);
        score += Math.min(16, distinctDomainCount(pending.sourceDomains()) * 4);
        score += Math.min(18, value(pending.routineTaskCount()) * 4 + value(pending.commercialObjectCount()) * 4 + value(pending.questionSignalCount()) * 2);
        if (containsConcreteMarker(pending.routineSummary() + " " + pending.painsSummary() + " " + pending.mechanismOpportunitiesSummary())) {
            score += 8;
        }
        return clamp(score);
    }

    /** Calcula confiança efetiva usando a confiança do sintetizador e a suficiência de fontes e sinais. */
    private int calculateConfidenceScore(RoutineQualityGatePending pending, int sourceCount, int signalCount) {
        int base = value(pending.cardConfidenceScore());
        int sourceScore = Math.min(25, sourceCount * 5);
        int signalScore = Math.min(25, signalCount * 2);
        int mixScore = Math.min(20, value(pending.painSignalCount()) * 4 + value(pending.mechanismOpportunityCount()) * 4 + value(pending.questionSignalCount()) * 2);
        return clamp(Math.round((base * 0.45f) + sourceScore + signalScore + mixScore));
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

    /** Monta notas objetivas com a causa da decisão para exibição e investigação. */
    private String buildNotes(RoutineQualityGatePending pending, String status, boolean hasMinimumSignalMix) {
        List<String> notes = new ArrayList<>();
        notes.add("status=" + status);
        notes.add("fontes=" + value(pending.sourceCount()));
        notes.add("sinais=" + value(pending.signalCount()));
        notes.add("perguntas=" + value(pending.questionSignalCount()));
        notes.add("dores=" + value(pending.painSignalCount()));
        notes.add("mecanismos=" + value(pending.mechanismOpportunityCount()));
        notes.add("tarefas=" + value(pending.routineTaskCount()));
        notes.add("objetosComerciais=" + value(pending.commercialObjectCount()));
        notes.add("mixMinimo=" + hasMinimumSignalMix);
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
        return (int) List.of(sourceDomains.split(","))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    /** Detecta marcadores concretos que normalmente indicam texto menos genérico. */
    private boolean containsConcreteMarker(String value) {
        return hasText(value) && value.matches(".*(\\d|WhatsApp|Instagram|agenda|cliente|preço|pacote|retorno|horário).*");
    }

    /** Normaliza texto para comparação simples de repetição. */
    private String normalize(String value) {
        return hasText(value) ? value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) : "";
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

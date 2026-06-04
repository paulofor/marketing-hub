package com.marketinghub.nichocnae.routinesynthesizer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Sintetiza um cartão de rotina objetivo a partir dos sinais estruturados coletados nas etapas anteriores. */
@Component
public class RoutineSynthesizerEngine {
    private static final int MAX_ITEMS_PER_BLOCK = 6;

    /** Monta o cartão de rotina sem criar oferta, campanha ou landing page. */
    public RoutineCardDraft synthesize(RoutineSynthesizerPending pending) {
        if (pending.signals() == null || pending.signals().isEmpty()) {
            throw new IllegalArgumentException("signals must contain at least one item");
        }
        List<SignalForRoutineSynthesis> signals = pending.signals().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::confidenceOrZero).reversed())
                .toList();
        String routine = buildBlock("Rotina operacional observada", pending, filterByType(signals, "ROUTINE", "TASK", "ROUTINE_TASK", "COMMERCIAL_TASK"));
        String pains = buildBlock("Dores e fricções recorrentes", pending, filterByType(signals, "PAIN", "PAIN_POINT", "CUSTOMER_QUESTION"));
        String results = buildBlock("Resultados buscados pelo público", pending, filterByType(signals, "RESULT", "RESULT_DESIRED", "DESIRED_RESULT"));
        String mechanisms = buildBlock("Oportunidades de mecanismo sem virar oferta", pending, filterByType(signals, "MECHANISM", "MECHANISM_OPPORTUNITY", "PROOF_SIGNAL", "LANGUAGE", "LANGUAGE_MARKER"));
        String evidence = buildEvidenceBlock(signals);
        String domains = signals.stream()
                .map(SignalForRoutineSynthesis::sourceDomain)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(12)
                .collect(Collectors.joining(", "));
        int confidence = Math.max(0, Math.min(100, (int) Math.round(signals.stream().mapToInt(this::confidenceOrZero).average().orElse(0))));
        return new RoutineCardDraft(routine, pains, results, mechanisms, evidence, domains.isBlank() ? "fontes não informadas" : domains, confidence);
    }

    /** Filtra sinais por tipo mantendo fallback para todos os sinais quando o bloco ficaria vazio. */
    private List<SignalForRoutineSynthesis> filterByType(List<SignalForRoutineSynthesis> signals, String... acceptedTypes) {
        Map<String, Boolean> accepted = List.of(acceptedTypes).stream().collect(Collectors.toMap(type -> type, type -> Boolean.TRUE));
        List<SignalForRoutineSynthesis> filtered = signals.stream()
                .filter(signal -> accepted.containsKey(normalizeType(signal.signalType())))
                .limit(MAX_ITEMS_PER_BLOCK)
                .toList();
        return filtered.isEmpty() ? signals.stream().limit(Math.min(3, signals.size())).toList() : filtered;
    }

    /** Monta um bloco textual curto do cartão citando sinais e evidências de apoio. */
    private String buildBlock(String title, RoutineSynthesizerPending pending, List<SignalForRoutineSynthesis> signals) {
        String bullets = signals.stream()
                .limit(MAX_ITEMS_PER_BLOCK)
                .map(signal -> "- " + clean(signal.signalText()) + " (evidência: " + clean(signal.evidenceExcerpt()) + ")")
                .collect(Collectors.joining("\n"));
        return title + " para " + pending.nicheName() + " / CNAE " + pending.cnaeCode() + ":\n" + bullets;
    }

    /** Monta o resumo de evidências destacando fontes e confiança dos sinais usados. */
    private String buildEvidenceBlock(List<SignalForRoutineSynthesis> signals) {
        return signals.stream()
                .limit(MAX_ITEMS_PER_BLOCK)
                .map(signal -> "- " + clean(signal.sourceDomain()) + " · " + confidenceOrZero(signal) + "% · " + clean(signal.evidenceExcerpt()))
                .collect(Collectors.joining("\n"));
    }

    /** Normaliza tipos de sinal para comparação estável. */
    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Retorna confiança numérica válida para ordenação e média. */
    private int confidenceOrZero(SignalForRoutineSynthesis signal) {
        return signal.confidenceScore() == null ? 0 : signal.confidenceScore();
    }

    /** Limpa quebras e espaços excessivos preservando conteúdo funcional. */
    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return "não informado";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}

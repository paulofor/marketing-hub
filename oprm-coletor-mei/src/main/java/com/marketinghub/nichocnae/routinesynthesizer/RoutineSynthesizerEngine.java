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

    /** Monta o cartão de rotina sem criar oferta, campanha, hipótese comercial ou recomendação de solução. */
    public RoutineCardDraft synthesize(RoutineSynthesizerPending pending) {
        if (pending.signals() == null || pending.signals().isEmpty()) {
            throw new IllegalArgumentException("signals must contain at least one item");
        }
        List<SignalForRoutineSynthesis> signals = pending.signals().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::confidenceOrZero).reversed())
                .toList();
        String routine = buildBlock("Rotina", pending,
                filterByType(signals, "ROUTINE_TASK", "ROUTINE", "TASK", "AUTONOMOUS_WORK_MODE"));
        String customerBehavior = buildBlock("Comportamento de clientes", pending,
                filterByType(signals, "CUSTOMER_ACQUISITION_BEHAVIOR", "CUSTOMER_QUESTION", "NICHE_OWNER_QUESTION", "FINAL_CUSTOMER_QUESTION", "QUESTION_SIGNAL"));
        String channels = buildBlock("Canais", pending,
                filterByType(signals, "CHANNEL_USAGE", "COMMERCIAL_TASK", "COMMERCIAL_OBJECT"));
        String operationalPains = buildBlock("Dores práticas", pending,
                filterByType(signals, "OPERATIONAL_PAIN", "OPERATIONAL_FRICTION", "PAIN_POINT", "PAIN_SIGNAL", "TIME_PRESSURE", "INCOME_INSTABILITY", "PRICE_INSECURITY", "CLIENT_NO_SHOW_OR_CANCELLATION"));
        String emotionalPains = buildBlock("Dores emocionais", pending,
                filterByType(signals, "EMOTIONAL_PAIN", "TRUST_REPUTATION_CONCERN", "STATUS_DESIRE"));
        String dreams = buildBlock("Sonhos", pending, filterByType(signals, "DREAM_SIGNAL", "RESULT_DESIRED"));
        String fears = buildBlock("Medos", pending, filterByType(signals, "FEAR_SIGNAL"));
        String language = buildBlock("Linguagem", pending,
                filterByType(signals, "LANGUAGE_MARKER", "CONTEXT_MARKER", "SEASONALITY_MARKER", "TRUST_REPUTATION_CONCERN"));
        String evidence = buildEvidenceBlock(signals);
        String domains = signals.stream()
                .map(SignalForRoutineSynthesis::sourceDomain)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(12)
                .collect(Collectors.joining(", "));
        int confidence = clamp((int) Math.round(signals.stream().mapToInt(this::confidenceOrZero).average().orElse(0)));
        int routineEvidenceScore = calculateTypeScore(signals, "ROUTINE_TASK", "ROUTINE", "TASK", "AUTONOMOUS_WORK_MODE", "CUSTOMER_ACQUISITION_BEHAVIOR", "CHANNEL_USAGE");
        int difficultyEvidenceScore = calculateTypeScore(signals, "OPERATIONAL_PAIN", "OPERATIONAL_FRICTION", "PAIN_POINT", "PAIN_SIGNAL", "EMOTIONAL_PAIN", "FEAR_SIGNAL", "TIME_PRESSURE", "INCOME_INSTABILITY", "PRICE_INSECURITY", "CLIENT_NO_SHOW_OR_CANCELLATION");
        int sourceDiversityScore = clamp(distinctDomainCount(domains) * 12);
        int solutionLanguageRiskScore = calculateSolutionLanguageRiskScore(signals);
        return new RoutineCardDraft(
                routine,
                customerBehavior,
                channels,
                operationalPains,
                emotionalPains,
                dreams,
                fears,
                language,
                operationalPains,
                customerBehavior + "\n" + channels,
                emotionalPains + "\n" + dreams + "\n" + fears + "\n" + language,
                evidence,
                domains.isBlank() ? "fontes não informadas" : domains,
                confidence,
                routineEvidenceScore,
                difficultyEvidenceScore,
                sourceDiversityScore,
                solutionLanguageRiskScore);
    }

    /** Filtra sinais por tipo mantendo fallback neutro somente para linguagem e contexto, nunca para solução. */
    private List<SignalForRoutineSynthesis> filterByType(List<SignalForRoutineSynthesis> signals, String... acceptedTypes) {
        Map<String, Boolean> accepted = List.of(acceptedTypes).stream().collect(Collectors.toMap(type -> type, type -> Boolean.TRUE));
        List<SignalForRoutineSynthesis> filtered = signals.stream()
                .filter(signal -> accepted.containsKey(normalizeType(signal.signalType())))
                .limit(MAX_ITEMS_PER_BLOCK)
                .toList();
        return filtered.isEmpty() ? List.of() : filtered;
    }

    /** Monta um bloco textual curto do cartão citando sinais e evidências de apoio sem sugerir produto ou oferta. */
    private String buildBlock(String title, RoutineSynthesizerPending pending, List<SignalForRoutineSynthesis> signals) {
        String bullets = signals.stream()
                .limit(MAX_ITEMS_PER_BLOCK)
                .map(signal -> "- " + clean(signal.signalText()) + " (evidência: " + clean(signal.evidenceExcerpt()) + ")")
                .collect(Collectors.joining("\n"));
        if (!StringUtils.hasText(bullets)) {
            bullets = "- Sem evidência suficiente neste bloco; manter pesquisa aberta até obter sinais auditáveis.";
        }
        return title + " para " + pending.nicheName() + " / CNAE " + pending.cnaeCode() + ":\n" + bullets;
    }

    /** Monta o resumo de evidências e alerta riscos de linguagem de solução em campo textual próprio do cartão. */
    private String buildEvidenceBlock(List<SignalForRoutineSynthesis> signals) {
        String evidence = signals.stream()
                .filter(signal -> !isSolutionRisk(signal))
                .limit(MAX_ITEMS_PER_BLOCK)
                .map(signal -> "- " + clean(signal.sourceDomain()) + " · " + confidenceOrZero(signal) + "% · " + clean(signal.evidenceExcerpt()))
                .collect(Collectors.joining("\n"));
        String risks = signals.stream()
                .filter(this::isSolutionRisk)
                .limit(MAX_ITEMS_PER_BLOCK)
                .map(signal -> "- Alerta de contaminação por solução em " + clean(signal.sourceDomain()) + ": " + clean(signal.evidenceExcerpt()))
                .collect(Collectors.joining("\n"));
        if (!StringUtils.hasText(evidence)) {
            evidence = "- Sem evidência de rotina suficiente sem risco de solução.";
        }
        return StringUtils.hasText(risks) ? evidence + "\nAlertas de contaminação por solução:\n" + risks : evidence;
    }

    /** Calcula pontuação média para um conjunto canônico de tipos de sinal. */
    private int calculateTypeScore(List<SignalForRoutineSynthesis> signals, String... acceptedTypes) {
        Map<String, Boolean> accepted = List.of(acceptedTypes).stream().collect(Collectors.toMap(type -> type, type -> Boolean.TRUE));
        return clamp((int) Math.round(signals.stream()
                .filter(signal -> accepted.containsKey(normalizeType(signal.signalType())))
                .mapToInt(this::confidenceOrZero)
                .average()
                .orElse(0)));
    }

    /** Calcula risco percentual de contaminação por linguagem de solução nos sinais usados. */
    private int calculateSolutionLanguageRiskScore(List<SignalForRoutineSynthesis> signals) {
        if (signals.isEmpty()) {
            return 0;
        }
        long riskCount = signals.stream().filter(this::isSolutionRisk).count();
        return clamp((int) Math.round((riskCount * 100.0) / signals.size()));
    }

    /** Identifica sinais que representam risco de solução ou contêm vocabulário típico de solução precoce. */
    private boolean isSolutionRisk(SignalForRoutineSynthesis signal) {
        String type = normalizeType(signal.signalType());
        String text = (clean(signal.signalText()) + " " + clean(signal.evidenceExcerpt())).toLowerCase(Locale.ROOT);
        return "SOLUTION_LANGUAGE_RISK".equals(type)
                || "MECHANISM_OPPORTUNITY".equals(type)
                || text.matches(".*(\\bia\\b|inteligência artificial|automação|software|sistema|app|ferramenta|curso).*");
    }

    /** Conta domínios distintos declarados na síntese para estimar variedade de fontes. */
    private int distinctDomainCount(String sourceDomains) {
        if (!StringUtils.hasText(sourceDomains)) {
            return 0;
        }
        return (int) List.of(sourceDomains.split(",")).stream().map(String::trim).filter(StringUtils::hasText).distinct().count();
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

    /** Garante que a pontuação calculada permaneça na escala percentual. */
    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}

package com.marketinghub.nichocnae.enrichednichematerializer;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Monta campos complementares determinísticos de rotina real sem gerar hipótese, oferta ou gatilho comercial. */
@Component
public class EnrichedNicheMaterializerEngine {
    private static final List<String> SOLUTION_LANGUAGE_TERMS = List.of(
            "ia", "inteligência artificial", "automação", "software", "sistema", "app", "ferramenta", "curso",
            "template", "oferta", "landing page");

    /** Deriva apenas contexto operacional auditável a partir do card aprovado no NichoCNAE. */
    public EnrichedNicheProfileDraft buildDraft(EnrichedNicheMaterializerPending pending) {
        return new EnrichedNicheProfileDraft(
                buildPersonaSummary(pending),
                buildLanguagePatterns(pending),
                null,
                null);
    }

    /** Resume a persona operacional observada sem inventar uma hipótese comercial. */
    private String buildPersonaSummary(EnrichedNicheMaterializerPending pending) {
        return "Profissional ou pequeno negócio ligado a " + pending.cnaeDescription()
                + " observado no modo " + pending.researchMode()
                + " com foco em rotina, tarefas, dificuldades e perguntas reais.";
    }

    /** Preserva linguagem pública a partir dos blocos de rotina, dificuldades, perguntas e contexto operacional. */
    private String buildLanguagePatterns(EnrichedNicheMaterializerPending pending) {
        return joinUseful(List.of(
                firstUsefulSentenceWithoutSolutionFrame(pending.painsSummary()),
                firstUsefulSentenceWithoutSolutionFrame(pending.routineSummary()),
                firstUsefulSentenceWithoutSolutionFrame(pending.resultsSummary()),
                firstUsefulSentenceWithoutSolutionFrame(pending.mechanismOpportunitiesSummary())));
    }

    /** Junta apenas frases com conteúdo útil. */
    private String joinUseful(List<String> values) {
        return values.stream().filter(this::hasText).reduce((left, right) -> left + "\n" + right).orElse(null);
    }

    /** Extrai a primeira frase útil quando ela não repete enquadramento de solução. */
    private String firstUsefulSentenceWithoutSolutionFrame(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        int dot = normalized.indexOf('.');
        int line = normalized.indexOf('-');
        int end = dot > 40 ? dot + 1 : (line > 20 ? line : Math.min(normalized.length(), 220));
        String sentence = normalized.substring(0, Math.min(end, normalized.length())).trim();
        return containsSolutionLanguage(sentence) ? null : sentence;
    }

    /** Detecta vocabulário de solução para não transformar esse texto em linguagem pública do perfil. */
    private boolean containsSolutionLanguage(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return SOLUTION_LANGUAGE_TERMS.stream().anyMatch(normalized::contains);
    }

    /** Verifica se existe texto útil no valor recebido. */
    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

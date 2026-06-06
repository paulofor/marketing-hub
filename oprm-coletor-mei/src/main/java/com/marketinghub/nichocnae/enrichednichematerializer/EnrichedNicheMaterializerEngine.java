package com.marketinghub.nichocnae.enrichednichematerializer;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Monta campos complementares determinísticos de rotina real sem gerar hipótese, oferta ou gatilho comercial. */
@Component
public class EnrichedNicheMaterializerEngine {
    /** Deriva apenas persona e linguagem operacional auditável a partir do card aprovado no NichoCNAE. */
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
                + " que vive a rotina descrita no NichoCNAE e busca reduzir esforço operacional percebido.";
    }

    /** Preserva linguagem do público a partir dos blocos de dores, rotina e resultados. */
    private String buildLanguagePatterns(EnrichedNicheMaterializerPending pending) {
        return joinUseful(List.of(
                firstUsefulSentence(pending.painsSummary()),
                firstUsefulSentence(pending.routineSummary()),
                firstUsefulSentence(pending.resultsSummary())));
    }

    /** Junta apenas frases com conteúdo útil. */
    private String joinUseful(List<String> values) {
        return values.stream().filter(this::hasText).reduce((left, right) -> left + "\n" + right).orElse(null);
    }

    /** Extrai a primeira frase útil de um bloco textual. */
    private String firstUsefulSentence(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        int dot = normalized.indexOf('.');
        int line = normalized.indexOf('-');
        int end = dot > 40 ? dot + 1 : (line > 20 ? line : Math.min(normalized.length(), 220));
        return normalized.substring(0, Math.min(end, normalized.length())).trim();
    }

    /** Verifica se existe texto útil no valor recebido. */
    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

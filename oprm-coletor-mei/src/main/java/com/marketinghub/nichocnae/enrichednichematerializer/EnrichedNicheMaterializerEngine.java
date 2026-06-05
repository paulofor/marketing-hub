package com.marketinghub.nichocnae.enrichednichematerializer;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Monta campos complementares determinísticos do nicho enriquecido sem gerar hipótese ou oferta. */
@Component
public class EnrichedNicheMaterializerEngine {
    /** Deriva persona, linguagem, gatilhos e objeções a partir do card aprovado no NichoCNAE. */
    public EnrichedNicheProfileDraft buildDraft(EnrichedNicheMaterializerPending pending) {
        return new EnrichedNicheProfileDraft(
                buildPersonaSummary(pending),
                buildLanguagePatterns(pending),
                buildCommercialTriggers(pending),
                buildObjections(pending));
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

    /** Identifica gatilhos comerciais seguros a partir de rotina, dores e resultados observados. */
    private String buildCommercialTriggers(EnrichedNicheMaterializerPending pending) {
        List<String> triggers = new ArrayList<>();
        triggers.add("Redução de esforço na rotina operacional");
        if (hasText(pending.painsSummary())) {
            triggers.add("Dor observada: " + firstUsefulSentence(pending.painsSummary()));
        }
        if (hasText(pending.resultsSummary())) {
            triggers.add("Resultado buscado: " + firstUsefulSentence(pending.resultsSummary()));
        }
        return String.join("\n", triggers);
    }

    /** Registra objeções prováveis sem criar oferta, preço ou promessa de hipótese. */
    private String buildObjections(EnrichedNicheMaterializerPending pending) {
        List<String> objections = new ArrayList<>();
        objections.add("Precisa enxergar aplicação prática no dia a dia antes de investir tempo.");
        objections.add("Pode desconfiar de solução genérica que não respeite a rotina do CNAE " + pending.cnaeCode() + ".");
        if (hasText(pending.evidenceSummary())) {
            objections.add("Exige evidência concreta: " + firstUsefulSentence(pending.evidenceSummary()));
        }
        return String.join("\n", objections);
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

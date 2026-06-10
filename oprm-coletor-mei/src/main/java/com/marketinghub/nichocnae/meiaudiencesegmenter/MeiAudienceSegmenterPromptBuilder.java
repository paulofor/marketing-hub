package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Constrói o prompt de IA para segmentar comportamento MEI/autônomo sem avançar para solução. */
@Component
public class MeiAudienceSegmenterPromptBuilder {
    private static final int MAX_ITEMS = 30;

    /** Monta prompt com CNAE, fontes, indicadores, sinais e bloqueios explícitos de produto/oferta. */
    public String buildPrompt(MeiAudienceSegmenterPending input) {
        return """
                Você é analista de comportamento de profissionais MEI/autônomos brasileiros.
                Tarefa: transformar evidências coletadas em segmentos comportamentais claros dentro de um CNAE, sem criar produto.

                Regras obrigatórias:
                - Descreva pessoas, rotina, dores, sonhos, medos, canais e linguagem real observada.
                - Separe públicos diferentes dentro do CNAE quando houver evidência.
                - Cada afirmação importante deve citar evidência resumida ou fonte no próprio texto.
                - Não escreva produto, oferta, preço, promessa, campanha, solução, software, automação, IA, curso ou ferramenta.
                - Não invente dado sem evidência; quando faltar dado, diga que a evidência é insuficiente.
                - Responda somente JSON aderente ao schema.

                CNAE: %s - %s
                Nome neutro: %s
                Nicho operacional: %s

                Cartão de rotina:
                %s

                Dores sintetizadas:
                %s

                Evidências sintetizadas:
                %s

                Fontes e indicadores:
                %s

                Sinais extraídos:
                %s
                """.formatted(
                clean(input.cnaeCode()),
                clean(input.cnaeDescription()),
                clean(input.neutralNicheName()),
                clean(input.nicheName()),
                clean(input.routineSummary()),
                clean(input.painsSummary()),
                clean(input.evidenceSummary()),
                sourceLines(input),
                signalLines(input));
    }

    /** Serializa fontes curtas com indicadores sem incluir HTML completo. */
    private String sourceLines(MeiAudienceSegmenterPending input) {
        if (input.sources() == null || input.sources().isEmpty()) {
            return "- Sem fontes curtas disponíveis.";
        }
        return input.sources().stream()
                .sorted(Comparator.comparing(this::sourceScore).reversed())
                .limit(MAX_ITEMS)
                .map(source -> "- snapshotId=%s domain=%s title=%s freshness=%s brazil=%s autonomous=%s outdatedRisk=%s corporateRisk=%s excerpt=%s"
                        .formatted(
                                source.sourceSnapshotId(),
                                clean(source.sourceDomain()),
                                clean(source.sourceTitle()),
                                source.sourceFreshnessScore(),
                                source.brazilRelevanceScore(),
                                source.autonomousProfessionalEvidenceScore(),
                                source.outdatedSourceRisk(),
                                source.structuredBusinessDriftRisk(),
                                clean(source.shortExcerpt())))
                .collect(Collectors.joining("\n"));
    }

    /** Serializa sinais com evidências rastreáveis e tipos canônicos. */
    private String signalLines(MeiAudienceSegmenterPending input) {
        if (input.signals() == null || input.signals().isEmpty()) {
            return "- Sem sinais extraídos disponíveis.";
        }
        return input.signals().stream()
                .sorted(Comparator.comparing(signal -> score(signal.confidenceScore()), Comparator.reverseOrder()))
                .limit(MAX_ITEMS)
                .map(signal -> "- signalId=%s type=%s domain=%s confidence=%s text=%s evidence=%s"
                        .formatted(
                                signal.extractedSignalId(),
                                clean(signal.signalType()),
                                clean(signal.sourceDomain()),
                                signal.confidenceScore(),
                                clean(signal.signalText()),
                                clean(signal.evidenceExcerpt())))
                .collect(Collectors.joining("\n"));
    }

    /** Pontua fonte para ordenar por Brasil, atualidade e evidência de autônomo. */
    private int sourceScore(SegmenterSourceSnapshot source) {
        return score(source.sourceFreshnessScore()) + score(source.brazilRelevanceScore()) + score(source.autonomousProfessionalEvidenceScore());
    }

    /** Retorna score seguro para ordenação. */
    private int score(Integer value) {
        return value == null ? 0 : value;
    }

    /** Limpa texto para reduzir ruído no prompt. */
    private String clean(String value) {
        return StringUtils.hasText(value) ? value.replaceAll("\\s+", " ").trim() : "não informado";
    }
}

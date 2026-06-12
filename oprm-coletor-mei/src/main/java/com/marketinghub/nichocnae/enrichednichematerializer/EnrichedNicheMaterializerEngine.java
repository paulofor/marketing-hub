package com.marketinghub.nichocnae.enrichednichematerializer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Monta campos complementares determinísticos de rotina real sem gerar produto, oferta ou promessa prematura. */
@Component
public class EnrichedNicheMaterializerEngine {
    private static final List<String> SOLUTION_LANGUAGE_TERMS = List.of(
            "ia", "inteligência artificial", "automação", "software", "sistema", "app", "ferramenta", "curso",
            "template", "oferta", "landing page", "produto", "promessa", "campanha", "anúncio", "vsl");

    private static final List<CommercialSignalRule> COMMERCIAL_TRIGGER_RULES = List.of(
            new CommercialSignalRule("agenda", "Gatilho comportamental: agenda vazia ou horários vagos percebidos na rotina."),
            new CommercialSignalRule("horário", "Gatilho comportamental: agenda vazia ou horários vagos percebidos na rotina."),
            new CommercialSignalRule("encaixe", "Gatilho comportamental: dificuldade de encaixar clientes sem bagunçar o atendimento."),
            new CommercialSignalRule("retorno", "Gatilho comportamental: perda de retorno de clientes depois do atendimento."),
            new CommercialSignalRule("recorr", "Gatilho comportamental: perda de retorno de clientes depois do atendimento."),
            new CommercialSignalRule("indicação", "Gatilho comportamental: dependência de indicação para manter movimento."),
            new CommercialSignalRule("boca a boca", "Gatilho comportamental: dependência de indicação para manter movimento."),
            new CommercialSignalRule("instabilidade", "Gatilho comportamental: medo de instabilidade na procura e na renda."),
            new CommercialSignalRule("oscila", "Gatilho comportamental: medo de instabilidade na procura e na renda."),
            new CommercialSignalRule("renda", "Gatilho comportamental: medo de instabilidade na procura e na renda."),
            new CommercialSignalRule("cobrar", "Gatilho comportamental: dificuldade de cobrar com segurança."),
            new CommercialSignalRule("preço", "Gatilho comportamental: dificuldade de cobrar com segurança."),
            new CommercialSignalRule("orçamento", "Gatilho comportamental: dificuldade de cobrar com segurança."),
            new CommercialSignalRule("desmarca", "Gatilho comportamental: cliente que desmarca e quebra a previsibilidade do dia."),
            new CommercialSignalRule("cancel", "Gatilho comportamental: cliente que desmarca e quebra a previsibilidade do dia."),
            new CommercialSignalRule("falta", "Gatilho comportamental: cliente que falta e quebra a previsibilidade do dia."),
            new CommercialSignalRule("tempo", "Gatilho comportamental: sobrecarga por falta de tempo na operação diária."),
            new CommercialSignalRule("organização", "Gatilho comportamental: sensação de desorganização em tarefas simples da rotina."));

    private static final List<CommercialSignalRule> OBJECTION_RULES = List.of(
            new CommercialSignalRule("agenda", "Objeção provável: receio de expor desorganização de horários para o cliente."),
            new CommercialSignalRule("horário", "Objeção provável: receio de expor desorganização de horários para o cliente."),
            new CommercialSignalRule("desmarca", "Objeção provável: acreditar que cancelamentos fazem parte normal do nicho."),
            new CommercialSignalRule("cancel", "Objeção provável: acreditar que cancelamentos fazem parte normal do nicho."),
            new CommercialSignalRule("cobrar", "Objeção provável: medo de parecer caro ou perder cliente ao cobrar melhor."),
            new CommercialSignalRule("preço", "Objeção provável: medo de parecer caro ou perder cliente ao cobrar melhor."),
            new CommercialSignalRule("tempo", "Objeção provável: dúvida se consegue manter uma rotina diferente sem aumentar o esforço diário."),
            new CommercialSignalRule("sobrecarga", "Objeção provável: dúvida se consegue manter uma rotina diferente sem aumentar o esforço diário."),
            new CommercialSignalRule("indicação", "Objeção provável: apego ao jeito atual baseado em indicação e relacionamento."),
            new CommercialSignalRule("boca a boca", "Objeção provável: apego ao jeito atual baseado em indicação e relacionamento."),
            new CommercialSignalRule("whatsapp", "Objeção provável: apego ao jeito atual baseado em conversa direta com clientes."),
            new CommercialSignalRule("instabilidade", "Objeção provável: acreditar que a instabilidade é normal e pouco controlável."),
            new CommercialSignalRule("renda", "Objeção provável: acreditar que a instabilidade é normal e pouco controlável."));

    /** Deriva apenas contexto operacional auditável a partir do card aprovado no NichoCNAE. */
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
                + " observado no modo " + pending.researchMode()
                + " com foco em rotina, tarefas, dificuldades e perguntas reais.";
    }

    /** Preserva linguagem pública a partir dos blocos de rotina, dificuldades, perguntas e contexto operacional. */
    private String buildLanguagePatterns(EnrichedNicheMaterializerPending pending) {
        List<String> usefulSentences = new ArrayList<>();
        usefulSentences.add(firstUsefulSentenceWithoutSolutionFrame(pending.painsSummary()));
        usefulSentences.add(firstUsefulSentenceWithoutSolutionFrame(pending.routineSummary()));
        usefulSentences.add(firstUsefulSentenceWithoutSolutionFrame(pending.resultsSummary()));
        usefulSentences.add(firstUsefulSentenceWithoutSolutionFrame(pending.mechanismOpportunitiesSummary()));
        return joinUseful(usefulSentences);
    }

    /** Deriva gatilhos comportamentais observáveis sem criar produto, promessa, campanha ou landing page. */
    private String buildCommercialTriggers(EnrichedNicheMaterializerPending pending) {
        return buildCommercialSignals(evidenceText(pending), COMMERCIAL_TRIGGER_RULES);
    }

    /** Deriva objeções comportamentais prováveis apenas a partir de evidências textuais do card aprovado. */
    private String buildObjections(EnrichedNicheMaterializerPending pending) {
        return buildCommercialSignals(evidenceText(pending), OBJECTION_RULES);
    }

    /** Junta os blocos públicos relevantes usados como evidência para campos comerciais não-ofertivos. */
    private String evidenceText(EnrichedNicheMaterializerPending pending) {
        return joinUseful(List.of(
                pending.painsSummary(),
                pending.resultsSummary(),
                pending.customerBehaviorSummary(),
                pending.channelsSummary(),
                pending.mechanismOpportunitiesSummary()));
    }

    /** Aplica regras determinísticas e remove qualquer síntese contaminada por linguagem de solução. */
    private String buildCommercialSignals(String evidenceText, List<CommercialSignalRule> rules) {
        if (!hasText(evidenceText)) {
            return null;
        }
        String normalizedEvidence = normalize(evidenceText);
        Set<String> signals = new LinkedHashSet<>();
        for (CommercialSignalRule rule : rules) {
            if (normalizedEvidence.contains(rule.evidenceTerm()) && !containsSolutionLanguage(rule.signal())) {
                signals.add(rule.signal());
            }
        }
        return joinUseful(new ArrayList<>(signals));
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
        String normalized = normalize(value);
        return SOLUTION_LANGUAGE_TERMS.stream().anyMatch(term -> containsTerm(normalized, term));
    }

    /** Compara termos curtos por palavra inteira para evitar falso positivo em palavras comuns. */
    private boolean containsTerm(String normalizedValue, String term) {
        if (term.length() <= 3) {
            return normalizedValue.matches(".*(^|[^\\p{L}\\p{N}])" + term + "([^\\p{L}\\p{N}]|$).*");
        }
        return normalizedValue.contains(term);
    }

    /** Normaliza texto para comparação determinística simples. */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    /** Verifica se existe texto útil no valor recebido. */
    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /** Regra de leitura comercial restrita a sinais comportamentais observáveis. */
    private record CommercialSignalRule(String evidenceTerm, String signal) {}
}

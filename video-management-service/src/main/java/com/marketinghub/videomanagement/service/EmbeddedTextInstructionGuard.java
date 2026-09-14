package com.marketinghub.videomanagement.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Responsabilidade: identificar ordens de texto embutido sem inverter proibições explícitas. */
final class EmbeddedTextInstructionGuard {
    private static final String VERBS = "mostrar|exibir|incluir|aplicar|gerar|inserir|desenhar|revelar";
    private static final String BOUNDARIES = "sem|nao|evitar|mas|porem|contudo|" + VERBS;
    private static final Pattern DIRECTIVE = Pattern.compile(
            "\\b(?:" + VERBS + ")\\b"
                    + "(?:\\s+(?!(?:" + BOUNDARIES + ")\\b)[a-z0-9]+){0,4}"
                    + "[\\s,]+\\b(?:textos?|legendas?|palavras?|precos?|logos?|cta escrito|interfaces?|letras?)\\b");
    private static final Pattern NEGATED_VERB_LIST = Pattern.compile(
            "(?s)(?:.*\\b(?:sem|nao|evitar)\\s+"
                    + "(?:[a-z]+(?:ar|er|ir|or)\\s*(?:,\\s*(?:(?:e|ou)\\s+)?|(?:e|ou)\\s+))*|.*\\bnem\\s+)$");

    /** Impede instanciação de um classificador sem estado. */
    private EmbeddedTextInstructionGuard() {}

    /** Avalia cada ordem separadamente; uma proibição anterior não libera outra ordem positiva. */
    static boolean isRequested(String objective) {
        if (objective == null || objective.isBlank()) return false;
        String text = Normalizer.normalize(objective, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        var instructions = DIRECTIVE.matcher(text);
        while (instructions.find()) {
            String preceding = text.substring(0, instructions.start());
            if (!NEGATED_VERB_LIST.matcher(preceding).matches()) return true;
        }
        return false;
    }
}

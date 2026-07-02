package com.marketinghub.worker.pipeline;

import java.util.Map;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver prompt/schema enviados pelo backend com fallback local do worker. */
public record BackendPromptSchemaTemplate(
        String templateKey,
        String version,
        String model,
        String schemaName,
        String promptMarkdownContent,
        String schemaJson) {

    /** Monta o template efetivo a partir do contrato pending e dos valores locais de fallback. */
    public static BackendPromptSchemaTemplate fromPromptData(
            Map<String, Object> promptData,
            String fallbackModel,
            String fallbackSchemaName,
            String fallbackPromptMarkdownContent,
            String fallbackSchemaJson) {
        Map<String, Object> template = promptData == null ? Map.of() : asMap(promptData.get("__promptTemplate"));
        return new BackendPromptSchemaTemplate(
                text(template.get("templateKey")),
                text(template.get("version")),
                firstText(template.get("model"), fallbackModel),
                firstText(template.get("schemaName"), fallbackSchemaName),
                firstText(template.get("promptMarkdownContent"), fallbackPromptMarkdownContent),
                firstText(template.get("schemaJson"), fallbackSchemaJson));
    }

    /** Retorna o primeiro texto preenchido entre o valor remoto e o fallback local. */
    private static String firstText(Object value, String fallback) {
        String text = text(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    /** Converte um valor textual opcional para string segura. */
    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    /** Normaliza um mapa vindo do JSON do backend. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}

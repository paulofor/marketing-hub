package com.marketinghub.moissaleslibraryworker.pipelines.shared.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/** Utilitário compartilhado para extrair texto funcional limpo de envelopes da OpenAI. */
public final class OpenAiTextResponseExtractor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Impede instanciação porque o extrator não mantém estado por execução. */
    private OpenAiTextResponseExtractor() {}

    /** Retorna o campo text/output_text quando existir; caso contário preserva o response bruto. */
    public static String extract(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return rawResponse;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
            String extracted = extractFromRoot(root);
            return StringUtils.hasText(extracted) ? extracted : rawResponse;
        } catch (JsonProcessingException ex) {
            return rawResponse;
        }
    }

    /** Busca o texto funcional nos formatos raiz usados pela Responses API. */
    private static String extractFromRoot(JsonNode root) {
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }
        String directOutput = extractFromOutput(root.path("output"));
        if (StringUtils.hasText(directOutput)) {
            return directOutput;
        }
        return extractFromContent(root.path("content"));
    }

    /** Varre a lista output da OpenAI procurando o primeiro conteúdo textual da mensagem. */
    private static String extractFromOutput(JsonNode output) {
        if (!output.isArray()) {
            return null;
        }
        for (JsonNode outputItem : output) {
            String text = extractFromContent(outputItem.path("content"));
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    /** Varre a lista content da OpenAI procurando o primeiro campo text preenchido. */
    private static String extractFromContent(JsonNode content) {
        if (!content.isArray()) {
            return null;
        }
        for (JsonNode contentItem : content) {
            JsonNode textNode = contentItem.path("text");
            if (textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
                return textNode.asText();
            }
        }
        return null;
    }
}

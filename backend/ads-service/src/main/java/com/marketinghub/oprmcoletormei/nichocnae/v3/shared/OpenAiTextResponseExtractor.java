package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/** Extrai o texto funcional de responses brutos da OpenAI para persistência limpa no NichoCNAE v3. */
final class OpenAiTextResponseExtractor {
    private final ObjectMapper objectMapper;

    /** Inicializa o extrator com o mapper JSON compartilhado pelo service da etapa. */
    OpenAiTextResponseExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Retorna o texto funcional da OpenAI quando existir ou mantém o response original como fallback auditável. */
    String extract(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return rawResponse;
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String extracted = extractFromRoot(root);
            return StringUtils.hasText(extracted) ? extracted : rawResponse;
        } catch (JsonProcessingException ex) {
            return rawResponse;
        }
    }

    /** Busca o primeiro campo text dentro dos formatos de response da OpenAI usados pelo executor. */
    private String extractFromRoot(JsonNode root) {
        String directOutput = extractFromOutput(root.path("output"));
        if (StringUtils.hasText(directOutput)) {
            return directOutput;
        }
        return extractFromContent(root.path("content"));
    }

    /** Varre a lista output da OpenAI procurando mensagens com content textual. */
    private String extractFromOutput(JsonNode output) {
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

    /** Varre a lista content da OpenAI procurando itens do tipo output_text com campo text. */
    private String extractFromContent(JsonNode content) {
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

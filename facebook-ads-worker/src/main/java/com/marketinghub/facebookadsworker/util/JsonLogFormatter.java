package com.marketinghub.facebookadsworker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Formata objetos em JSON para logs estruturados sem expor credenciais.
 */
public final class JsonLogFormatter {
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
        "access_token",
        "accesstoken",
        "systemuseraccesstoken",
        "appsecret"
    );

    private static final ObjectMapper FALLBACK_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private JsonLogFormatter() {
    }

    /**
     * Encapsula um valor para serialização JSON usando o mapper informado.
     */
    public static Object wrap(ObjectMapper mapper, Object value) {
        return new JsonLogValue(mapper, value);
    }

    /**
     * Encapsula um valor para serialização JSON usando o mapper padrão.
     */
    public static Object wrap(Object value) {
        return new JsonLogValue(null, value);
    }

    /**
     * Serializa o valor em JSON mascarando campos sensíveis conhecidos.
     */
    private static String toJson(ObjectMapper mapper, Object value) {
        if (value == null) {
            return "null";
        }
        ObjectMapper selectedMapper = mapper != null ? mapper : FALLBACK_MAPPER;
        if (mapper != null) {
            try {
                return selectedMapper.writeValueAsString(sanitize(selectedMapper.valueToTree(value)));
            } catch (Exception ignored) {
                // Fallback to default mapper below.
            }
        }
        try {
            return FALLBACK_MAPPER.writeValueAsString(sanitize(FALLBACK_MAPPER.valueToTree(value)));
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    /**
     * Mascara recursivamente os campos sensíveis antes de escrever o log.
     */
    private static JsonNode sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), "***");
                } else {
                    sanitize(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(JsonLogFormatter::sanitize);
        }
        return node;
    }

    /**
     * Identifica nomes de campos que representam credenciais ou segredos.
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName
            .replace("-", "")
            .replace("_", "")
            .toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_NAMES.contains(normalized) || fieldName.toLowerCase(Locale.ROOT).contains("token");
    }

    /**
     * Valor adiado que só serializa quando o logger realmente renderiza a mensagem.
     */
    private static final class JsonLogValue {
        private final ObjectMapper mapper;
        private final Object value;

        /**
         * Guarda o mapper e o valor original para serialização tardia.
         */
        private JsonLogValue(ObjectMapper mapper, Object value) {
            this.mapper = mapper;
            this.value = value;
        }

        /**
         * Renderiza o valor em JSON mascarado para o log.
         */
        @Override
        public String toString() {
            return toJson(mapper, value);
        }
    }
}

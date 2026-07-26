package com.marketinghub.mcpserver.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Responsabilidade: mascarar segredos antes que respostas operacionais sejam devolvidas pelo MCP.
 */
@Service
public class SensitiveDataSanitizer {

    private static final String MASK = "[REDACTED]";
    private static final Pattern JSON_OR_FORM_SECRET = Pattern.compile(
            "(?i)([\"']?(?:access[_-]?token|fb[_-]?exchange[_-]?token|client[_-]?secret|app[_-]?secret|"
                    + "appsecret[_-]?proof|authorization|api[_-]?key|secret|token)[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s,}]+)([\"']?)"
    );
    private static final Pattern URL_SECRET = Pattern.compile(
            "(?i)([?&](?:access[_-]?token|fb[_-]?exchange[_-]?token|client[_-]?secret|app[_-]?secret|"
                    + "appsecret[_-]?proof|authorization|api[_-]?key|secret|token)=)([^&\\s\"'}]+)"
    );
    private static final Pattern AUTH_HEADER_SECRET = Pattern.compile(
            "(?i)((?:Authorization|authorization)\\s*[:=]\\s*(?:Bearer|OAuth)\\s+)([^\\s\"',}]+)"
    );

    /**
     * Mascara valores sensíveis preservando a estrutura do objeto recebido.
     */
    public Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }
        if (value instanceof List<?> list) {
            return sanitizeList(list);
        }
        if (value instanceof String text) {
            return sanitizeText(text);
        }
        return value;
    }

    /**
     * Mascara uma string que possa conter JSON, form-urlencoded, URL ou header com segredo.
     */
    public String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String sanitized = AUTH_HEADER_SECRET.matcher(text).replaceAll("$1" + MASK);
        sanitized = JSON_OR_FORM_SECRET.matcher(sanitized).replaceAll("$1" + MASK + "$3");
        sanitized = URL_SECRET.matcher(sanitized).replaceAll("$1" + MASK);
        return sanitized;
    }

    /**
     * Mascara mapas recursivamente e substitui valores inteiros quando a chave é sensível.
     */
    private Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (isSensitiveKey(key)) {
                sanitized.put(key, MASK);
            } else {
                sanitized.put(key, sanitize(entry.getValue()));
            }
        }
        return sanitized;
    }

    /**
     * Mascara listas recursivamente preservando a ordem original.
     */
    private List<Object> sanitizeList(List<?> list) {
        List<Object> sanitized = new ArrayList<>(list.size());
        for (Object item : list) {
            sanitized.add(sanitize(item));
        }
        return sanitized;
    }

    /**
     * Identifica nomes de campos que normalmente carregam credenciais ou tokens operacionais.
     */
    private boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("-", "_");
        return normalized.equals("token")
                || normalized.endsWith("_token")
                || normalized.contains("access_token")
                || normalized.contains("fb_exchange_token")
                || normalized.contains("client_secret")
                || normalized.contains("app_secret")
                || normalized.contains("appsecret_proof")
                || normalized.contains("authorization")
                || normalized.contains("api_key")
                || normalized.equals("secret")
                || normalized.endsWith("_secret");
    }
}

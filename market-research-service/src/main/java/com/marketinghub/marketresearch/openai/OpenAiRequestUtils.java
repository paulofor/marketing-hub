package com.marketinghub.marketresearch.openai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenAiRequestUtils {
    private OpenAiRequestUtils() {
    }

    public static Map<String, Object> message(String role, String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", List.of(textContent(text)));
        return message;
    }

    public static Map<String, Object> textContent(String text) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "input_text");
        content.put("text", text);
        return content;
    }

    public static void maybeAddReasoning(Map<String, Object> payload, String model) {
        if (requiresReasoning(model)) {
            payload.put("reasoning", Map.of("effort", "medium"));
        }
    }

    public static boolean requiresReasoning(String model) {
        if (model == null) {
            return false;
        }
        String normalized = model.toLowerCase();
        return normalized.startsWith("o");
    }
}

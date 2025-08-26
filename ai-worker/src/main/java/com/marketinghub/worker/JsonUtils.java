package com.marketinghub.worker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidades para lidar com JSON retornado por LLMs que pode vir
 * embrulhado em cercas Markdown ou duplamente codificado.
 */
final class JsonUtils {
  private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Remove cercas ``` e ```json caso a resposta venha em Markdown. */
  static String stripCodeFences(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.startsWith("```")) {
      t = t.replaceAll("(?s)^```(?:json)?\\s*", "");
      t = t.replaceAll("(?s)\\s*```$", "");
    }
    return t.trim();
  }

  /** Faz parse resiliente, inclusive para JSON duplamente codificado. */
  static <T> T parsePossiblyDoubleEncoded(String raw, TypeReference<T> type) throws JsonProcessingException {
    String cleaned = stripCodeFences(raw);
    try {
      try {
        return MAPPER.readValue(cleaned, type);
      } catch (JsonParseException ignored) {
        // Plano B: a resposta está como STRING contendo JSON
        String inner = MAPPER.readValue(cleaned, String.class);
        return MAPPER.readValue(inner, type);
      }
    } catch (JsonProcessingException e) {
      String preview = preview(raw);
      log.error("Failed to parse JSON: {}", preview, e);
      throw e;
    }
  }

  private static String preview(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.length() > 200 ? t.substring(0, 200) + "..." : t;
  }
}

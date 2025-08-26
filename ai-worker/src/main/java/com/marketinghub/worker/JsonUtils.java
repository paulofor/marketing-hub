package com.marketinghub.worker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilitário para lidar com respostas do modelo que podem vir embrulhadas em
 * cercas Markdown ou duplamente codificadas como string JSON.
 */
public final class JsonUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Remove cercas ``` e ```json caso a resposta venha em Markdown. */
  public static String stripCodeFences(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.startsWith("```") ) {
      t = t.replaceAll("(?s)^```(?:json)?\\s*", "");
      t = t.replaceAll("(?s)\\s*```$", "");
    }
    return t.trim();
  }

  /** Faz parse resiliente, inclusive para JSON duplamente codificado. */
  public static <T> T parsePossiblyDoubleEncoded(String raw, TypeReference<T> type) throws JsonProcessingException {
    String cleaned = stripCodeFences(raw);
    try {
      return MAPPER.readValue(cleaned, type);
    } catch (JsonParseException ignored) {
      // Plano B: a resposta está como STRING contendo JSON
      String inner = MAPPER.readValue(cleaned, String.class);
      return MAPPER.readValue(inner, type);
    }
  }
}
